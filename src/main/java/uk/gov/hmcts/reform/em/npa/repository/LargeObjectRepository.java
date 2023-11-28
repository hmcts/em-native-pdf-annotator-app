package uk.gov.hmcts.reform.em.npa.repository;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class LargeObjectRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void removeLargeObjects(Integer limit) {
        Query query = entityManager.createNativeQuery(
            "SELECT lo_unlink(l.oid) FROM pg_catalog.pg_largeobject_metadata l limit :limit");
        query.setParameter("limit", limit);
        query.getResultList();
    }
}