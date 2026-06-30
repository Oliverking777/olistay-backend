package olistay.backend.repository;

import olistay.backend.entity.Property;
import olistay.backend.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    /**
     * All images for a property, ordered for consistent display sequence.
     */
    List<PropertyImage> findAllByPropertyOrderByUploadOrderAsc(Property property);

    /**
     * The single primary image for a property — used by list/browse views
     * so we don't have to load and filter the whole images collection just
     * to show one thumbnail.
     */
    Optional<PropertyImage> findFirstByPropertyAndIsPrimaryTrue(Property property);

    /**
     * Count of images already attached to a property — used to determine
     * upload_order for a newly uploaded image and whether it should become
     * the primary (count == 0 means this is the first image).
     */
    long countByProperty(Property property);
}