package olistay.backend.service;

import olistay.backend.dto.PropertyImageResponseDTO;
import olistay.backend.dto.PropertyRequestDTO;
import olistay.backend.dto.PropertyResponseDTO;
import olistay.backend.dto.PropertySummaryDTO;
import olistay.backend.enums.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PropertyService {

    /**
     * Calls the AI Engine's rent predictor for a property the HOST is
     * actively creating (no property_id required — see RentPredictRequestDTO
     * javadoc). Used to show a "suggested rent" hint in the create-listing
     * form before the HOST commits to a price.
     */
    olistay.backend.dto.ml.RentPredictResponseDTO suggestRent(PropertyRequestDTO request);

    /**
     * Computes the total cost of occupancy (rent + utilities + transport +
     * advance/caution) for a property against the authenticated tenant's
     * saved financial profile. Calls POST /financial/hidden-costs. Throws
     * ResourceNotFoundException if the tenant has no financial profile yet.
     */
    olistay.backend.dto.ml.HiddenCostsResponseDTO calculateHiddenCosts(String tenantEmail, Long propertyId);

    /**
     * Scores how well a specific property fits the authenticated tenant,
     * using their saved TenantFinancialProfile and the property's saved
     * data. Calls POST /scoring/score. Throws ResourceNotFoundException if
     * the tenant has no financial profile yet — scoring requires one.
     */
    olistay.backend.dto.ml.ScoringResponseDTO scoreProperty(String tenantEmail, Long propertyId);


    /**
     * Runs the full four-stage recommendation pipeline for the authenticated
     * tenant against all currently AVAILABLE properties in their target
     * city (or all cities if cityFilter is null). Calls
     * POST /recommender/recommend. Throws ResourceNotFoundException if the
     * tenant has no financial profile yet.
     */
    olistay.backend.dto.ml.RecommendResponseDTO recommendProperties(
            String tenantEmail, String cityFilter, Integer topN
    );

    /**
     * Creates a new listing owned by the authenticated HOST, optionally with
     * one or more images uploaded at the same time. New listings start in
     * UNDER_REVIEW status pending admin approval.
     *
     * @param images may be null or empty — a listing can be created without
     *               images and have them added later via uploadImages().
     *               The first file in the list (if any) becomes the primary
     *               image, in upload order.
     */
    PropertyResponseDTO createProperty(String hostEmail, PropertyRequestDTO request, List<MultipartFile> images);

    /**
     * Returns a single property by ID, regardless of status, with its full
     * images collection — used for the owning HOST's own dashboard and for
     * ADMIN moderation. Public browse callers should use
     * getAvailablePropertyById() instead.
     */
    PropertyResponseDTO getPropertyById(Long id);

    /**
     * Returns a single property by ID, but only if it is AVAILABLE, with
     * its full images collection. Used for public-facing property detail
     * pages — this is the "get by ID, show all images" endpoint.
     */
    PropertyResponseDTO getAvailablePropertyById(Long id);

    /**
     * Full update — replaces every field. Only the owning HOST or an ADMIN
     * may perform this. Throws PropertyOwnershipException otherwise.
     * Does not touch images — use the dedicated image endpoints for that.
     */
    PropertyResponseDTO updateProperty(String currentUserEmail, Long id, PropertyRequestDTO request);

    /**
     * Deletes a listing, its images rows, and their Cloudinary assets.
     * Only the owning HOST or an ADMIN may perform this.
     */
    void deleteProperty(String currentUserEmail, Long id);

    /**
     * Adds one or more images to an existing property. If the property
     * currently has zero images, the first file in this batch becomes
     * primary. Only the owning HOST or an ADMIN may perform this.
     */
    List<PropertyImageResponseDTO> uploadImages(String currentUserEmail, Long propertyId, List<MultipartFile> images);

    /**
     * Removes a single image from a property (DB row + Cloudinary asset).
     * If the deleted image was primary, the next image by uploadOrder
     * (if any) is automatically promoted to primary.
     */
    void deleteImage(String currentUserEmail, Long propertyId, Long imageId);

    /**
     * Default browse feed — all AVAILABLE listings, paginated, summary view
     * (primary image only — this is the "get properties, show primary image" endpoint).
     */
    Page<PropertySummaryDTO> browseAvailable(Pageable pageable);

    /**
     * Browse AVAILABLE listings filtered by city — summary view.
     */
    Page<PropertySummaryDTO> browseByCity(String city, Pageable pageable);

    /**
     * Browse AVAILABLE listings filtered by city and neighbourhood — summary view.
     */
    Page<PropertySummaryDTO> browseByCityAndNeighbourhood(
            String city, String neighbourhood, Pageable pageable
    );

    /**
     * Keyword search across title, description, neighbourhood — AVAILABLE
     * only, summary view.
     */
    Page<PropertySummaryDTO> searchByKeyword(String keyword, Pageable pageable);

    /**
     * Combined filter search backing SearchPage.jsx's filter bar — keyword,
     * city, property type(s), minimum bedrooms/bathrooms, and price range,
     * all optional and combinable. AVAILABLE only, summary view.
     */
    Page<PropertySummaryDTO> searchWithFilters(
            olistay.backend.dto.PropertySearchFilter filter, Pageable pageable
    );

    /**
     * Returns every listing owned by the authenticated HOST, regardless of
     * status, summary view — populates the host's "My Listings" dashboard.
     */
    List<PropertySummaryDTO> getMyListings(String hostEmail);

    /**
     * Changes a property's status. Used by the owning HOST (e.g. marking
     * OCCUPIED, or ARCHIVED) and by ADMIN (e.g. approving from
     * UNDER_REVIEW to AVAILABLE).
     */
    PropertyResponseDTO updateStatus(String currentUserEmail, Long id, PropertyStatus newStatus);
}