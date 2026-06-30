package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single image attached to a Property listing, hosted on Cloudinary.
 *
 * Ordering / primary selection: the first image uploaded for a property is
 * automatically flagged isPrimary=true. Display order on the frontend is
 * driven by uploadOrder (ascending), not by database insertion order alone,
 * so reordering is possible later without relying on row order.
 */
@Entity
@Table(name = "property_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    /**
     * Secure HTTPS URL returned by Cloudinary after upload — what the
     * frontend actually renders.
     */
    @Column(nullable = false, length = 1000)
    private String imageUrl;

    /**
     * Cloudinary's public_id for this asset — required to delete or
     * transform the image later via the Cloudinary API. Never exposed
     * to the frontend; used internally by CloudinaryService only.
     */
    @Column(nullable = false, length = 500)
    private String cloudinaryPublicId;

    /**
     * True for exactly one image per property: the first one uploaded.
     * Returned alone in list/browse views; all images are returned together
     * in the single-property detail view.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Upload sequence — 0 for the first image, incrementing from there.
     * Drives display order independent of DB row insertion order.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer uploadOrder = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
