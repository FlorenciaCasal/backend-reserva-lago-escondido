package com.luismunozse.reservalago.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_advance_gallery_items")
@Getter
@Setter
public class ProjectAdvanceGalleryItem extends AbstractMediaGalleryItem {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advance_id", nullable = false)
    private ProjectAdvance advance;
}
