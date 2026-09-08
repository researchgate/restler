package net.researchgate.restler.service.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.model.ServiceModel;
import net.researchgate.restler.domain.ValidatedEntity;
import net.researchgate.restler.service.dao.ValidatedEntityDao;
import org.bson.types.ObjectId;

/**
 * Model for managing validated entities
 */

@Singleton
public class ValidatedEntityModel extends ServiceModel<ValidatedEntity, ObjectId> {
    @Inject
    public ValidatedEntityModel(ValidatedEntityDao validatedEntityDao) {
        super(validatedEntityDao);
    }
}
