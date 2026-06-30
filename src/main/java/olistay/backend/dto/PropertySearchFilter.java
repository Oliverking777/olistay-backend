package olistay.backend.dto;

import olistay.backend.enums.PropertyType;

import java.util.List;

/**
 * Query parameters for GET /properties/filter, bound from request params
 * by PropertyController (not a request body — this is a GET endpoint so
 * search/filter state stays bookmarkable/shareable via the URL, matching
 * how SearchPage.jsx mirrors filters into useSearchParams).
 *
 * Every field is optional. A null/empty field means "don't filter on this" —
 * see PropertyRepository.searchWithFilters() for how that's applied at the
 * query level.
 *
 * propertyTypes accepts multiple values (?propertyTypes=APARTMENT&propertyTypes=HOUSE)
 * since the frontend's Property Type dropdown is a multi-select checkbox list.
 */
public record PropertySearchFilter(
        String keyword,
        String city,
        List<PropertyType> propertyTypes,
        Integer minBedrooms,
        Integer minBathrooms,
        Double minPrice,
        Double maxPrice
) {
}
