package net.researchgate.restler.service.resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.PatchContext;
import net.researchgate.restdsl.resources.ServiceResource;
import net.researchgate.restler.domain.ValidatedEntity;
import net.researchgate.restler.service.model.ValidatedEntityModel;
import org.bson.types.ObjectId;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Validated entity resource
 */
@Path("validated-entities")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ValidatedEntityResource extends ServiceResource<ValidatedEntity, ObjectId> {
    @Inject
    public ValidatedEntityResource(ValidatedEntityModel validatedEntityModel) throws RestDslException {
        super(validatedEntityModel, ValidatedEntity.class, ObjectId.class);
    }

    @Override
    protected void validatePostEntity(ValidatedEntity entity) throws RestDslException {
        super.validatePostEntity(entity);

        if (entity.getNotNullField() == null) {
            throw new RestDslException("notNullField must not be null", RestDslException.Type.ENTITY_ERROR);
        }
        if (entity.getPositiveField() != null && entity.getPositiveField() <= 0) {
            throw new RestDslException("positiveField must be positive", RestDslException.Type.ENTITY_ERROR);
        }
    }

    @Override
    protected void validatePatch(ValidatedEntity entity, PatchContext context) throws RestDslException {
        super.validatePatch(entity, context);

        if (context.getUnsetFields().contains("notNullField")) {
            throw new RestDslException("notNullField must not be set to null", RestDslException.Type.ENTITY_ERROR);
        }

        if (entity.getPositiveField() != null && entity.getPositiveField() <= 0) {
            throw new RestDslException("positiveField must be set to positive values or unset", RestDslException.Type.ENTITY_ERROR);
        }
    }

    @Override
    protected void validatePut(ObjectId key, ValidatedEntity entity) throws RestDslException {
        super.validatePut(key, entity);

        if (entity.getNotNullField() == null) {
            throw new RestDslException("notNullField must not be null", RestDslException.Type.ENTITY_ERROR);
        }

        if (entity.getPositiveField() != null && entity.getPositiveField() <= 0) {
            throw new RestDslException("positiveField must be positive", RestDslException.Type.ENTITY_ERROR);
        }
    }
}
