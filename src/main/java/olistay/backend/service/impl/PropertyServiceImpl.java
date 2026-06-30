package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.client.MlEngineClient;
import olistay.backend.dto.PropertyImageResponseDTO;
import olistay.backend.dto.PropertyRequestDTO;
import olistay.backend.dto.PropertyResponseDTO;
import olistay.backend.dto.PropertySummaryDTO;
import olistay.backend.dto.ml.PropertyMlFeaturesDTO;
import olistay.backend.dto.ml.RentPredictRequestDTO;
import olistay.backend.dto.ml.RentPredictResponseDTO;
import olistay.backend.entity.Property;
import olistay.backend.entity.PropertyImage;
import olistay.backend.entity.User;
import olistay.backend.enums.PropertyStatus;
import olistay.backend.enums.Role;
import olistay.backend.exception.PropertyOwnershipException;
import olistay.backend.exception.ResourceNotFoundException;
import olistay.backend.repository.PropertyImageRepository;
import olistay.backend.dto.PropertySearchFilter;
import olistay.backend.repository.PropertyRepository;
import olistay.backend.repository.UserRepository;
import olistay.backend.service.CloudinaryService;
import olistay.backend.service.PropertyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final MlEngineClient mlEngineClient;
    private final olistay.backend.service.TenantFinancialProfileService tenantFinancialProfileService;

    @Override
    @Transactional
    public PropertyResponseDTO createProperty(
            String hostEmail, PropertyRequestDTO request, List<MultipartFile> images
    ) {
        User host = userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only HOST or ADMIN can create listings — GUESTs must go through
        // the become-host flow first. Belt-and-suspenders check: SecurityConfig
        // already requires authentication on this route, but role is enforced
        // here where the loaded User entity is available.
        if (host.getRole() == Role.GUEST) {
            throw new PropertyOwnershipException(
                    "Only hosts can list properties. Complete the Become a Landlord flow first."
            );
        }

        Property property = buildPropertyFromRequest(request, host);
        Property saved = propertyRepository.save(property);

        List<PropertyImageResponseDTO> uploadedImages = List.of();
        if (images != null && !images.isEmpty()) {
            uploadedImages = uploadImages(hostEmail, saved.getId(), images);
        }

        return PropertyResponseDTO.fromEntity(saved, uploadedImages);
    }

    @Override
    public RentPredictResponseDTO suggestRent(PropertyRequestDTO request) {
        // Build an in-memory, unpersisted Property so we can reuse the
        // existing buildPropertyFromRequest()/PropertyMlFeaturesDTO mapping
        // logic rather than duplicating field-by-field mapping a second time.
        // host=null is safe here since PropertyMlFeaturesDTO.fromEntity()
        // never touches the host field, and this entity is never saved.
        Property unsavedProperty = buildPropertyFromRequest(request, null);

        // area_m2 is normally derived in @PrePersist, which never fires for
        // an unsaved entity — derive it manually here so size_m2/area_m2
        // aren't sent as null to the rent predictor.
        if (unsavedProperty.getLengthM() != null && unsavedProperty.getWidthM() != null) {
            unsavedProperty.setAreaM2(unsavedProperty.getLengthM() * unsavedProperty.getWidthM());
        }

        PropertyMlFeaturesDTO features = PropertyMlFeaturesDTO.fromEntity(unsavedProperty);
        RentPredictRequestDTO rentRequest = RentPredictRequestDTO.fromMlFeatures(features);

        return mlEngineClient.predictRent(rentRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public olistay.backend.dto.ml.ScoringResponseDTO scoreProperty(String tenantEmail, Long propertyId) {
        Property property = findByIdOrThrow(propertyId);

        olistay.backend.entity.TenantFinancialProfile financialProfile =
                tenantFinancialProfileService.getProfileEntity(tenantEmail);

        olistay.backend.dto.ml.ScoringTenantDataDTO tenantData = buildScoringTenantData(financialProfile);

        PropertyMlFeaturesDTO features = PropertyMlFeaturesDTO.fromEntity(property);
        olistay.backend.dto.ml.ScoringPropertyDataDTO propertyData =
                olistay.backend.dto.ml.ScoringPropertyDataDTO.fromMlFeatures(features);

        olistay.backend.dto.ml.ScoringRequestDTO scoringRequest =
                new olistay.backend.dto.ml.ScoringRequestDTO(tenantData, propertyData);

        return mlEngineClient.scoreProperty(scoringRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public olistay.backend.dto.ml.HiddenCostsResponseDTO calculateHiddenCosts(String tenantEmail, Long propertyId) {
        Property property = findByIdOrThrow(propertyId);

        olistay.backend.entity.TenantFinancialProfile financialProfile =
                tenantFinancialProfileService.getProfileEntity(tenantEmail);

        // Effective income (base + additional income sources), resolved via
        // the shared helper rather than recomputed inline — same resolution
        // logic used by buildScoringTenantData(), since hidden_costs.py's
        // tenant_monthly_income is a single flat field with no
        // additional_income_sources support, just like scoring/recommend.
        double effectiveIncome = tenantFinancialProfileService.resolveEffectiveMonthlyIncome(tenantEmail);

        PropertyMlFeaturesDTO features = PropertyMlFeaturesDTO.fromEntity(property);

        olistay.backend.dto.ml.HiddenCostsRequestDTO hiddenCostsRequest =
                olistay.backend.dto.ml.HiddenCostsRequestDTO.fromMlFeatures(
                        features,
                        financialProfile.getCurrentNeighbourhood(),
                        effectiveIncome
                );

        return mlEngineClient.calculateHiddenCosts(hiddenCostsRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public olistay.backend.dto.ml.RecommendResponseDTO recommendProperties(
            String tenantEmail, String cityFilter, Integer topN
    ) {
        olistay.backend.entity.TenantFinancialProfile financialProfile =
                tenantFinancialProfileService.getProfileEntity(tenantEmail);

        olistay.backend.dto.ml.ScoringTenantDataDTO scoringTenant = buildScoringTenantData(financialProfile);
        olistay.backend.dto.ml.RecommendTenantDTO recommendTenant =
                olistay.backend.dto.ml.RecommendTenantDTO.fromScoringTenant(scoringTenant);

        // Candidate pool: all AVAILABLE properties, optionally filtered by
        // city. Pulling the full set (rather than pre-filtering server-side
        // by rent) is intentional — Stage 1 of the Python pipeline
        // (apply_knowledge_filter) already does the affordability gating;
        // duplicating that cutoff here would just be a second, possibly
        // inconsistent copy of the same business rule.
        List<Property> candidates = (cityFilter == null || cityFilter.isBlank())
                ? propertyRepository.findAllByStatus(PropertyStatus.AVAILABLE, Pageable.unpaged()).getContent()
                : propertyRepository.findAllByStatusAndCityIgnoreCase(
                PropertyStatus.AVAILABLE, cityFilter, Pageable.unpaged()
        ).getContent();

        List<PropertyMlFeaturesDTO> candidateFeatures = candidates.stream()
                .map(PropertyMlFeaturesDTO::fromEntity)
                .toList();

        olistay.backend.dto.ml.RecommendRequestDTO recommendRequest = olistay.backend.dto.ml.RecommendRequestDTO.of(
                recommendTenant,
                candidateFeatures,
                topN != null ? topN : 10
        );

        return mlEngineClient.recommend(recommendRequest);
    }

    /**
     * Builds the scoring/recommend tenant payload from a TenantFinancialProfile,
     * resolving effective income/obligations/savings (handling the itemised
     * breakdown overrides that /scoring/score and /recommender/recommend
     * don't natively support — see TenantFinancialProfileServiceImpl javadoc
     * for the full rationale). custom_weights is left null — letting the ML
     * engine use its expert-default or learned weights rather than Spring
     * dictating scoring weights.
     */
    private olistay.backend.dto.ml.ScoringTenantDataDTO buildScoringTenantData(
            olistay.backend.entity.TenantFinancialProfile profile
    ) {
        double effectiveIncome = profile.getMonthlyIncome() != null ? profile.getMonthlyIncome() : 0.0;
        double additionalIncome = profile.getAdditionalIncomeSources() == null ? 0.0
                : profile.getAdditionalIncomeSources().stream()
                .mapToDouble(s -> s.getMonthlyAmount() != null ? s.getMonthlyAmount() : 0.0)
                .sum();
        effectiveIncome += additionalIncome;

        double effectiveObligations = profile.getExpenseBreakdown() != null
                ? profile.getExpenseBreakdown().total()
                : orDefault(profile.getFixedObligations(), 0.0);

        double effectiveSavings = profile.getAvailableFundsBreakdown() != null
                ? profile.getAvailableFundsBreakdown().total()
                : orDefault(profile.getCurrentSavings(), 0.0);

        return new olistay.backend.dto.ml.ScoringTenantDataDTO(
                String.valueOf(profile.getUser().getId()),
                effectiveIncome,
                effectiveObligations,
                profile.getSavingsGoal(),
                profile.getGoalTimelineMonths(),
                profile.getHouseholdSize(),
                effectiveSavings,
                profile.getHasDependents(),
                orDefault(profile.getNeedsParking(), false),
                orDefault(profile.getNeedsSchoolNearby(), false),
                orDefault(profile.getNeedsHospitalNearby(), false),
                orDefault(profile.getNeedsGenerator(), false),
                profile.getCurrentNeighbourhood(),
                profile.getCurrentCity(),
                profile.getJobSector(),
                profile.getIncomeStability(),
                null // custom_weights — let the ML engine use its own defaults
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponseDTO getPropertyById(Long id) {
        Property property = findByIdOrThrow(id);
        return PropertyResponseDTO.fromEntity(property, loadImages(property));
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponseDTO getAvailablePropertyById(Long id) {
        Property property = findByIdOrThrow(id);
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Property not found or not currently available");
        }
        return PropertyResponseDTO.fromEntity(property, loadImages(property));
    }

    @Override
    @Transactional
    public PropertyResponseDTO updateProperty(String currentUserEmail, Long id, PropertyRequestDTO request) {
        Property property = findByIdOrThrow(id);
        assertOwnerOrAdmin(currentUserEmail, property);

        applyRequestToProperty(request, property);

        Property saved = propertyRepository.save(property);
        return PropertyResponseDTO.fromEntity(saved, loadImages(saved));
    }

    @Override
    @Transactional
    public void deleteProperty(String currentUserEmail, Long id) {
        Property property = findByIdOrThrow(id);
        assertOwnerOrAdmin(currentUserEmail, property);

        // Delete Cloudinary assets first — if this partially fails, the DB
        // delete still proceeds (see CloudinaryService.delete() javadoc on
        // why a failed remote delete shouldn't block the local transaction).
        List<PropertyImage> images = propertyImageRepository.findAllByPropertyOrderByUploadOrderAsc(property);
        images.forEach(img -> cloudinaryService.delete(img.getCloudinaryPublicId()));

        // orphanRemoval=true on Property.images cascades the row deletes;
        // explicit deleteAll above only handles the remote Cloudinary side.
        propertyRepository.delete(property);
    }

    @Override
    @Transactional
    public List<PropertyImageResponseDTO> uploadImages(
            String currentUserEmail, Long propertyId, List<MultipartFile> images
    ) {
        Property property = findByIdOrThrow(propertyId);
        assertOwnerOrAdmin(currentUserEmail, property);

        if (images == null || images.isEmpty()) {
            throw new olistay.backend.exception.ImageUploadException("No image files provided");
        }

        long existingCount = propertyImageRepository.countByProperty(property);
        String folder = "olistay/properties/" + property.getId();

        List<PropertyImage> saved = new java.util.ArrayList<>();
        int order = (int) existingCount;

        for (MultipartFile file : images) {
            CloudinaryService.CloudinaryUploadResult result = cloudinaryService.upload(file, folder);

            // First image ever uploaded for this property (existingCount == 0
            // AND this is the first file in the current batch) becomes primary.
            boolean isPrimary = (existingCount == 0 && order == 0);

            PropertyImage image = PropertyImage.builder()
                    .property(property)
                    .imageUrl(result.secureUrl())
                    .cloudinaryPublicId(result.publicId())
                    .isPrimary(isPrimary)
                    .uploadOrder(order)
                    .build();

            saved.add(propertyImageRepository.save(image));
            order++;
        }

        return saved.stream().map(PropertyImageResponseDTO::fromEntity).toList();
    }

    @Override
    @Transactional
    public void deleteImage(String currentUserEmail, Long propertyId, Long imageId) {
        Property property = findByIdOrThrow(propertyId);
        assertOwnerOrAdmin(currentUserEmail, property);

        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!image.getProperty().getId().equals(propertyId)) {
            throw new ResourceNotFoundException("Image does not belong to this property");
        }

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());

        cloudinaryService.delete(image.getCloudinaryPublicId());
        propertyImageRepository.delete(image);

        // Promote the next image (by uploadOrder) to primary if the deleted
        // one was primary — a property with remaining images should never
        // end up with zero primary images.
        if (wasPrimary) {
            propertyImageRepository.findAllByPropertyOrderByUploadOrderAsc(property)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        propertyImageRepository.save(next);
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySummaryDTO> browseAvailable(Pageable pageable) {
        return propertyRepository.findAllByStatus(PropertyStatus.AVAILABLE, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySummaryDTO> browseByCity(String city, Pageable pageable) {
        return propertyRepository.findAllByStatusAndCityIgnoreCase(
                PropertyStatus.AVAILABLE, city, pageable
        ).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySummaryDTO> browseByCityAndNeighbourhood(
            String city, String neighbourhood, Pageable pageable
    ) {
        return propertyRepository.findAllByStatusAndCityIgnoreCaseAndNeighbourhoodIgnoreCase(
                PropertyStatus.AVAILABLE, city, neighbourhood, pageable
        ).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySummaryDTO> searchByKeyword(String keyword, Pageable pageable) {
        return propertyRepository.searchByKeyword(PropertyStatus.AVAILABLE, keyword, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySummaryDTO> searchWithFilters(PropertySearchFilter filter, Pageable pageable) {
        String keyword = blankToNull(filter.keyword());
        String city    = blankToNull(filter.city());
        List<olistay.backend.enums.PropertyType> propertyTypes =
                (filter.propertyTypes() == null || filter.propertyTypes().isEmpty())
                        ? null
                        : filter.propertyTypes();

        return propertyRepository.searchWithFilters(
                PropertyStatus.AVAILABLE,
                keyword,
                city,
                propertyTypes,
                filter.minBedrooms(),
                filter.minBathrooms(),
                filter.minPrice(),
                filter.maxPrice(),
                pageable
        ).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertySummaryDTO> getMyListings(String hostEmail) {
        User host = userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return propertyRepository.findAllByHost(host)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public PropertyResponseDTO updateStatus(String currentUserEmail, Long id, PropertyStatus newStatus) {
        Property property = findByIdOrThrow(id);

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = property.getHost().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new PropertyOwnershipException("You do not have permission to modify this property");
        }

        if (!isAdmin && newStatus == PropertyStatus.AVAILABLE
                && property.getStatus() == PropertyStatus.UNDER_REVIEW) {
            throw new PropertyOwnershipException(
                    "New listings require admin approval before becoming available"
            );
        }

        property.setStatus(newStatus);
        Property saved = propertyRepository.save(property);
        return PropertyResponseDTO.fromEntity(saved, loadImages(saved));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Property findByIdOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    private void assertOwnerOrAdmin(String currentUserEmail, Property property) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = property.getHost().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new PropertyOwnershipException("You do not have permission to modify this property");
        }
    }

    /**
     * Builds the summary DTO for list views, resolving only the primary
     * image URL via a targeted query rather than loading the full lazy
     * images collection on every row of a paginated result.
     */
    private PropertySummaryDTO toSummary(Property property) {
        String primaryImageUrl = propertyImageRepository
                .findFirstByPropertyAndIsPrimaryTrue(property)
                .map(PropertyImage::getImageUrl)
                .orElse(null);
        return PropertySummaryDTO.fromEntity(property, primaryImageUrl);
    }

    /**
     * Loads the full ordered images list for detail views (getById).
     */
    private List<PropertyImageResponseDTO> loadImages(Property property) {
        return propertyImageRepository.findAllByPropertyOrderByUploadOrderAsc(property)
                .stream()
                .map(PropertyImageResponseDTO::fromEntity)
                .toList();
    }

    private Property buildPropertyFromRequest(PropertyRequestDTO request, User host) {
        return Property.builder()
                .host(host)
                .title(request.title())
                .description(request.description())
                .status(PropertyStatus.UNDER_REVIEW)
                .neighbourhood(request.neighbourhood())
                .city(request.city())
                .gpsLat(request.gpsLat())
                .gpsLon(request.gpsLon())
                .infraZone(request.infraZone())
                .propertyType(request.propertyType())
                .unitType(request.unitType())
                .lengthM(request.lengthM())
                .widthM(request.widthM())
                .numBedrooms(orDefault(request.numBedrooms(), 0))
                .numBathrooms(orDefault(request.numBathrooms(), 0))
                .floorLevel(orDefault(request.floorLevel(), 0))
                .sharedWc(orDefault(request.sharedWc(), false))
                .hasParking(orDefault(request.hasParking(), false))
                .hasGenerator(orDefault(request.hasGenerator(), false))
                .hasWaterMeter(orDefault(request.hasWaterMeter(), true))
                .fiberInternet(orDefault(request.fiberInternet(), false))
                .securityGate(orDefault(request.securityGate(), false))
                .hasGardien(orDefault(request.hasGardien(), false))
                .roadFrontageM(orDefault(request.roadFrontageM(), 0.0))
                .shopfrontQuality(orDefault(request.shopfrontQuality(), 0))
                .loadingBay(orDefault(request.loadingBay(), false))
                .standbyPowerKva(orDefault(request.standbyPowerKva(), 0.0))
                .nearSchool(orDefault(request.nearSchool(), false))
                .nearMarket(orDefault(request.nearMarket(), false))
                .nearHospital(orDefault(request.nearHospital(), false))
                .nearHighway(orDefault(request.nearHighway(), false))
                .nearUniversity(orDefault(request.nearUniversity(), false))
                .structuralQuality(orDefault(request.structuralQuality(), 5))
                .conditionScore(orDefault(request.conditionScore(), 5))
                .buildYear(request.buildYear())
                .floodRisk(orDefault(request.floodRisk(), false))
                .noiseLevel(orDefault(request.noiseLevel(), 5))
                .titleType(request.titleType())
                .advanceMonths(orDefault(request.advanceMonths(), 3))
                .cautionMonths(orDefault(request.cautionMonths(), 1))
                .rentXaf(request.rentXaf())
                .landlordReputation(orDefault(request.landlordReputation(), 5))
                .leaseSecurity(orDefault(request.leaseSecurity(), 5))
                .transportScore(orDefault(request.transportScore(), 5))
                .build();
    }

    private void applyRequestToProperty(PropertyRequestDTO request, Property property) {
        property.setTitle(request.title());
        property.setDescription(request.description());
        property.setNeighbourhood(request.neighbourhood());
        property.setCity(request.city());
        property.setGpsLat(request.gpsLat());
        property.setGpsLon(request.gpsLon());
        property.setInfraZone(request.infraZone());
        property.setPropertyType(request.propertyType());
        property.setUnitType(request.unitType());
        property.setLengthM(request.lengthM());
        property.setWidthM(request.widthM());
        property.setNumBedrooms(orDefault(request.numBedrooms(), property.getNumBedrooms()));
        property.setNumBathrooms(orDefault(request.numBathrooms(), property.getNumBathrooms()));
        property.setFloorLevel(orDefault(request.floorLevel(), property.getFloorLevel()));
        property.setSharedWc(orDefault(request.sharedWc(), property.getSharedWc()));
        property.setHasParking(orDefault(request.hasParking(), property.getHasParking()));
        property.setHasGenerator(orDefault(request.hasGenerator(), property.getHasGenerator()));
        property.setHasWaterMeter(orDefault(request.hasWaterMeter(), property.getHasWaterMeter()));
        property.setFiberInternet(orDefault(request.fiberInternet(), property.getFiberInternet()));
        property.setSecurityGate(orDefault(request.securityGate(), property.getSecurityGate()));
        property.setHasGardien(orDefault(request.hasGardien(), property.getHasGardien()));
        property.setRoadFrontageM(orDefault(request.roadFrontageM(), property.getRoadFrontageM()));
        property.setShopfrontQuality(orDefault(request.shopfrontQuality(), property.getShopfrontQuality()));
        property.setLoadingBay(orDefault(request.loadingBay(), property.getLoadingBay()));
        property.setStandbyPowerKva(orDefault(request.standbyPowerKva(), property.getStandbyPowerKva()));
        property.setNearSchool(orDefault(request.nearSchool(), property.getNearSchool()));
        property.setNearMarket(orDefault(request.nearMarket(), property.getNearMarket()));
        property.setNearHospital(orDefault(request.nearHospital(), property.getNearHospital()));
        property.setNearHighway(orDefault(request.nearHighway(), property.getNearHighway()));
        property.setNearUniversity(orDefault(request.nearUniversity(), property.getNearUniversity()));
        property.setStructuralQuality(orDefault(request.structuralQuality(), property.getStructuralQuality()));
        property.setConditionScore(orDefault(request.conditionScore(), property.getConditionScore()));
        property.setBuildYear(request.buildYear());
        property.setFloodRisk(orDefault(request.floodRisk(), property.getFloodRisk()));
        property.setNoiseLevel(orDefault(request.noiseLevel(), property.getNoiseLevel()));
        property.setTitleType(request.titleType());
        property.setAdvanceMonths(orDefault(request.advanceMonths(), property.getAdvanceMonths()));
        property.setCautionMonths(orDefault(request.cautionMonths(), property.getCautionMonths()));
        property.setRentXaf(request.rentXaf());
        property.setLandlordReputation(orDefault(request.landlordReputation(), property.getLandlordReputation()));
        property.setLeaseSecurity(orDefault(request.leaseSecurity(), property.getLeaseSecurity()));
        property.setTransportScore(orDefault(request.transportScore(), property.getTransportScore()));
    }

    private <T> T orDefault(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}