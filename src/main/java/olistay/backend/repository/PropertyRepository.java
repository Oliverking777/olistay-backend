package olistay.backend.repository;

import olistay.backend.entity.Property;
import olistay.backend.entity.User;
import olistay.backend.enums.PropertyStatus;
import olistay.backend.enums.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    // ── Public browsing queries ───────────────────────────────────────────────

    Page<Property> findAllByStatus(PropertyStatus status, Pageable pageable);

    Page<Property> findAllByStatusAndCityIgnoreCase(
            PropertyStatus status,
            String city,
            Pageable pageable
    );

    Page<Property> findAllByStatusAndCityIgnoreCaseAndNeighbourhoodIgnoreCase(
            PropertyStatus status,
            String city,
            String neighbourhood,
            Pageable pageable
    );

    Page<Property> findAllByStatusAndCityIgnoreCaseAndPropertyType(
            PropertyStatus status,
            String city,
            PropertyType propertyType,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Property p
            WHERE p.status = :status
              AND (
                  LOWER(p.title)       LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(p.neighbourhood) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              )
            """)
    Page<Property> searchByKeyword(
            @Param("status") PropertyStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Property p
            WHERE p.status   = :status
              AND p.city     = :city
              AND p.rentXaf <= :maxRent
            """)
    List<Property> findAffordableByCity(
            @Param("status")  PropertyStatus status,
            @Param("city")    String city,
            @Param("maxRent") Double maxRent
    );

    @Query("""
            SELECT p FROM Property p
            WHERE p.status   = :status
              AND p.city     = :city
              AND p.rentXaf <= :maxRent
            """)
    Page<Property> findCandidatesForPipeline(
            @Param("status")  PropertyStatus status,
            @Param("city")    String city,
            @Param("maxRent") Double maxRent,
            Pageable pageable
    );

    /**
     * Combined frontend search filter — backs GET /properties/filter.
     *
     * Every filter parameter is OPTIONAL: pass null to skip that condition
     * entirely (the "(:param IS NULL OR ...)" pattern below). This lets the
     * single query serve the full range of SearchPage.jsx filter
     * combinations (keyword + city + property type + bed/bath minimums +
     * price range) without building a dynamic Specification/Criteria query.
     *
     * IMPORTANT — explicit CAST(... AS string) on every string parameter
     * used inside LOWER()/LIKE: when a bind parameter is only ever compared
     * via "IS NULL OR ..." (i.e. it can legitimately be null), Hibernate
     * sends it to PostgreSQL without a concrete type hint. PostgreSQL's
     * JDBC driver then defaults that parameter to bytea, and
     * "LOWER(bytea)" has no matching function — this throws
     * "function lower(bytea) does not exist" at runtime, but only once a
     * request actually passes keyword/city as null (e.g. the unfiltered
     * default search). The explicit CAST(:param AS string) forces Hibernate
     * to send the parameter as text regardless of whether the value is
     * null, which resolves it. Do not remove these casts even though the
     * query "looks" identical without them — it will break the moment
     * keyword or city is omitted.
     *
     * Always scoped to AVAILABLE — this is a public discovery endpoint.
     */
    @Query("""
            SELECT p FROM Property p
            WHERE p.status = :status
              AND (:keyword IS NULL OR
                   LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(p.neighbourhood) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(p.city) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              )
              AND (:city IS NULL OR LOWER(p.city) = LOWER(CAST(:city AS string)))
              AND (:propertyTypes IS NULL OR p.propertyType IN :propertyTypes)
              AND (:minBedrooms IS NULL OR p.numBedrooms >= :minBedrooms)
              AND (:minBathrooms IS NULL OR p.numBathrooms >= :minBathrooms)
              AND (:minPrice IS NULL OR p.rentXaf >= :minPrice)
              AND (:maxPrice IS NULL OR p.rentXaf <= :maxPrice)
            """)
    Page<Property> searchWithFilters(
            @Param("status") PropertyStatus status,
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("propertyTypes") List<PropertyType> propertyTypes,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("minBathrooms") Integer minBathrooms,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );

    // ── Host management queries ───────────────────────────────────────────────

    List<Property> findAllByHost(User host);

    List<Property> findAllByHostAndStatus(User host, PropertyStatus status);

    long countByHostAndStatus(User host, PropertyStatus status);
}