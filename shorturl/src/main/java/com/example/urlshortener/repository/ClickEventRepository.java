package com.example.urlshortener.repository;

import com.example.urlshortener.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortCode(String shortCode);

    /** Top referrers for a code, most frequent first. */
    @Query("""
            select coalesce(c.referrer, '(direct)') as referrer, count(c) as clicks
            from ClickEvent c
            where c.shortCode = :shortCode
            group by c.referrer
            order by count(c) desc
            """)
    List<ReferrerCount> topReferrers(@Param("shortCode") String shortCode);

    /** Projection for the grouped referrer query. */
    interface ReferrerCount {
        String getReferrer();
        long getClicks();
    }
}
