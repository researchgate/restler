package net.researchgate.restler.service.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.morphia.Datastore;
import net.researchgate.restdsl.dao.MongoServiceDao;
import net.researchgate.restler.domain.ValidatedEntity;
import org.bson.types.ObjectId;

@Singleton
public class ValidatedEntityDao extends MongoServiceDao<ValidatedEntity, ObjectId> {
    @Inject
    public ValidatedEntityDao(Datastore datastore) {
        super(datastore, ValidatedEntity.class);
    }
}
