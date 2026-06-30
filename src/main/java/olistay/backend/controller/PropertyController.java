package olistay.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olistay.backend.dto.PropertyImageResponseDTO;
import olistay.backend.dto.PropertyRequestDTO;
import olistay.backend.dto.PropertyResponseDTO;
import olistay.backend.dto.PropertySearchFilter;
import olistay.backend.dto.PropertySummaryDTO;
import olistay.backend.enums.PropertyStatus;
import olistay.backend.enums.PropertyType;
import olistay.backend.service.PropertyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Property listing endpoints.
 *
 * Route split:
 *   GET  /properties, /properties/{id}, /properties/city/**, /properties/search
 *        — public, no auth required (configured in SecurityConfig)
 *   POST /properties, PUT/DELETE/.../status, /properties/me, /properties/{id}/images
 *        — authenticated; ownership/role checks happen in the service layer
 *
 * List endpoints (browseAvailable, browseByCity, search, getMyListings) return
 * PropertySummaryDTO — primary image only, to keep paginated payloads small.
 * Detail endpoints (getById, getAvailableProperty) return PropertyResponseDTO
 * — the full record including every uploaded image.
 */
@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final ObjectMapper objectMapper;

    /**
     * POST /properties
     * multipart/form-data with two parts:
     *   - "property": JSON body matching PropertyRequestDTO
     *   - "images":   zero or more image files (first file becomes primary)
     *
     * Caller must be HOST or ADMIN — enforced in PropertyServiceImpl since
     * the role check needs the loaded User entity.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PropertyResponseDTO> createProperty(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("property") String propertyJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        PropertyRequestDTO request = parsePropertyJson(propertyJson);
        PropertyResponseDTO created = propertyService.createProperty(
                userDetails.getUsername(), request, images
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /properties/suggest-rent
     * Calls the AI Engine's XGBoost rent predictor for the property data
     * the HOST has filled out so far, before the listing is saved. Used to
     * show a "suggested rent" hint in the create-listing form. Requires
     * authentication (any authenticated user, not just HOST — a prospective
     * HOST mid-way through the "Become a Landlord" + create-listing flow may
     * call this before their role promotion has technically completed).
     */
    @PostMapping("/suggest-rent")
    public ResponseEntity<olistay.backend.dto.ml.RentPredictResponseDTO> suggestRent(
            @Valid @RequestBody PropertyRequestDTO request
    ) {
        return ResponseEntity.ok(propertyService.suggestRent(request));
    }

    /**
     * GET /properties/{id}/score
     * Scores how well this property fits the authenticated tenant, using
     * their saved financial profile. 404 if the tenant has no financial
     * profile yet — create one via POST /tenant/financial-profile first.
     */
    @GetMapping("/{id}/score")
    public ResponseEntity<olistay.backend.dto.ml.ScoringResponseDTO> scoreProperty(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(propertyService.scoreProperty(userDetails.getUsername(), id));
    }

    /**
     * GET /properties/{id}/hidden-costs
     * Computes the total cost of occupancy for this property (rent +
     * utilities + transport + advance/caution) against the authenticated
     * tenant's saved financial profile — the "true cost beyond the sticker
     * price" view on a property detail page. 404 if the tenant has no
     * financial profile yet.
     */
    @GetMapping("/{id}/hidden-costs")
    public ResponseEntity<olistay.backend.dto.ml.HiddenCostsResponseDTO> calculateHiddenCosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(propertyService.calculateHiddenCosts(userDetails.getUsername(), id));
    }

    /**
     * GET /properties/recommendations?city=Yaound%C3%A9&topN=10
     * Runs the full hybrid recommendation pipeline against all AVAILABLE
     * properties (optionally filtered by city) for the authenticated
     * tenant. 404 if the tenant has no financial profile yet.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<olistay.backend.dto.ml.RecommendResponseDTO> recommendProperties(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer topN
    ) {
        return ResponseEntity.ok(
                propertyService.recommendProperties(userDetails.getUsername(), city, topN)
        );
    }

    /**
     * GET /properties/{id}
     * Public detail view — only returns AVAILABLE properties, with the full
     * images collection ("get by id, show all images").
     */
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> getAvailableProperty(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getAvailablePropertyById(id));
    }

    /**
     * GET /properties
     * Default browse feed — all AVAILABLE listings, paginated, primary
     * image only ("get properties, show primary image").
     */
    @GetMapping
    public ResponseEntity<Page<PropertySummaryDTO>> browseAvailable(Pageable pageable) {
        return ResponseEntity.ok(propertyService.browseAvailable(pageable));
    }

    /**
     * GET /properties/city/{city}
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<Page<PropertySummaryDTO>> browseByCity(
            @PathVariable String city,
            Pageable pageable
    ) {
        return ResponseEntity.ok(propertyService.browseByCity(city, pageable));
    }

    /**
     * GET /properties/city/{city}/neighbourhood/{neighbourhood}
     */
    @GetMapping("/city/{city}/neighbourhood/{neighbourhood}")
    public ResponseEntity<Page<PropertySummaryDTO>> browseByCityAndNeighbourhood(
            @PathVariable String city,
            @PathVariable String neighbourhood,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                propertyService.browseByCityAndNeighbourhood(city, neighbourhood, pageable)
        );
    }

    /**
     * GET /properties/search?keyword=...
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PropertySummaryDTO>> searchByKeyword(
            @RequestParam String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(propertyService.searchByKeyword(keyword, pageable));
    }

    /**
     * GET /properties/filter
     *
     * Combined search/filter endpoint backing SearchPage.jsx's filter bar.
     * All parameters are optional and combinable:
     *   keyword          — matches title, description, neighbourhood, or city
     *   city             — exact match, case-insensitive
     *   propertyTypes    — repeatable: ?propertyTypes=APARTMENT&propertyTypes=STUDIO
     *   minBedrooms      — inclusive lower bound  ("2+" beds filter)
     *   minBathrooms     — inclusive lower bound  ("1.5+" baths filter — stored as int so 1.5 → 1)
     *   minPrice/maxPrice — inclusive rentXaf bounds in XAF
     *
     * Public, no auth required. Returns AVAILABLE listings only, summary
     * view (primary image only) to keep paginated payloads small.
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<PropertySummaryDTO>> searchWithFilters(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) List<PropertyType> propertyTypes,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Pageable pageable
    ) {
        PropertySearchFilter filter = new PropertySearchFilter(
                keyword, city, propertyTypes, minBedrooms, minBathrooms, minPrice, maxPrice
        );
        return ResponseEntity.ok(propertyService.searchWithFilters(filter, pageable));
    }

    /**
     * GET /properties/me
     * The authenticated HOST's full portfolio, regardless of status.
     */
    @GetMapping("/me")
    public ResponseEntity<List<PropertySummaryDTO>> getMyListings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(propertyService.getMyListings(userDetails.getUsername()));
    }

    /**
     * GET /properties/{id}/details
     * Full detail view regardless of status — for the owning HOST or ADMIN
     * to preview/manage a listing that isn't (or is no longer) AVAILABLE.
     * Requires authentication (enforced via SecurityConfig's /properties/**
     * authenticated() catch-all, since this path isn't in the public matcher list).
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<PropertyResponseDTO> getPropertyDetails(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    /**
     * PUT /properties/{id}
     * Full field update. Does not touch images — use the image endpoints
     * below for that. Only the owning HOST or an ADMIN may perform this.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> updateProperty(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequestDTO request
    ) {
        return ResponseEntity.ok(
                propertyService.updateProperty(userDetails.getUsername(), id, request)
        );
    }

    /**
     * PATCH /properties/{id}/status?status=AVAILABLE
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PropertyResponseDTO> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam PropertyStatus status
    ) {
        return ResponseEntity.ok(
                propertyService.updateStatus(userDetails.getUsername(), id, status)
        );
    }

    /**
     * DELETE /properties/{id}
     * Deletes the property, its image rows, and their Cloudinary assets.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        propertyService.deleteProperty(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /properties/{id}/images
     * Adds one or more images to an existing property. If the property has
     * zero images before this call, the first file uploaded becomes primary.
     */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PropertyImageResponseDTO>> uploadImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images
    ) {
        List<PropertyImageResponseDTO> uploaded = propertyService.uploadImages(
                userDetails.getUsername(), id, images
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
    }

    /**
     * DELETE /properties/{id}/images/{imageId}
     * Removes a single image. If it was primary, the next image by upload
     * order is automatically promoted.
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        propertyService.deleteImage(userDetails.getUsername(), id, imageId);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Manually deserializes the "property" multipart part as JSON, since
     * @RequestPart can't apply @Valid bean validation to a String part the
     * way it can to a JSON request body. Validation constraints on
     * PropertyRequestDTO still fire on update (@RequestBody, JSON-only
     * endpoint) but not on create (multipart) — see class-level note if you
     * want to add manual validation here later via a Validator bean.
     */
    private PropertyRequestDTO parsePropertyJson(String propertyJson) {
        try {
            return objectMapper.readValue(propertyJson, PropertyRequestDTO.class);
        } catch (IOException e) {
            throw new olistay.backend.exception.InvalidRequestException(
                    "Invalid property JSON payload: " + e.getMessage()
            );
        }
    }
}
