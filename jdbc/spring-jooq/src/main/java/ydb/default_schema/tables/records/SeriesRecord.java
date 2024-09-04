/*
 * This file is generated by jOOQ.
 */
package ydb.default_schema.tables.records;


import java.time.LocalDate;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import ydb.default_schema.tables.Series;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class SeriesRecord extends UpdatableRecordImpl<SeriesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>DEFAULT_SCHEMA.series.series_id</code>.
     */
    public void setSeriesId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>DEFAULT_SCHEMA.series.series_id</code>.
     */
    public Long getSeriesId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>DEFAULT_SCHEMA.series.title</code>.
     */
    public void setTitle(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>DEFAULT_SCHEMA.series.title</code>.
     */
    public String getTitle() {
        return (String) get(1);
    }

    /**
     * Setter for <code>DEFAULT_SCHEMA.series.series_info</code>.
     */
    public void setSeriesInfo(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>DEFAULT_SCHEMA.series.series_info</code>.
     */
    public String getSeriesInfo() {
        return (String) get(2);
    }

    /**
     * Setter for <code>DEFAULT_SCHEMA.series.release_date</code>.
     */
    public void setReleaseDate(LocalDate value) {
        set(3, value);
    }

    /**
     * Getter for <code>DEFAULT_SCHEMA.series.release_date</code>.
     */
    public LocalDate getReleaseDate() {
        return (LocalDate) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached SeriesRecord
     */
    public SeriesRecord() {
        super(Series.SERIES);
    }

    /**
     * Create a detached, initialised SeriesRecord
     */
    public SeriesRecord(Long seriesId, String title, String seriesInfo, LocalDate releaseDate) {
        super(Series.SERIES);

        setSeriesId(seriesId);
        setTitle(title);
        setSeriesInfo(seriesInfo);
        setReleaseDate(releaseDate);
        resetChangedOnNotNull();
    }
}
