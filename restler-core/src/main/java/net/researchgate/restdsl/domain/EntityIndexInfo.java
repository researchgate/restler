package net.researchgate.restdsl.domain;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import net.researchgate.restdsl.types.TypeInfoUtil;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Information about indexes - gets indices from mongo and translates them to Java names
 */
public class EntityIndexInfo<V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityIndexInfo.class);

    private Set<String> indexesMap = new LinkedHashSet<>();
    private Set<String> indexPrefixMap = new LinkedHashSet<>();

    public EntityIndexInfo(Class<V> entityClazz, List<DBObject> indexInfo) {
        List<String> indexStrings = new ArrayList<>();
        for (DBObject dbObject : indexInfo) {
            BasicDBObject dbo = (BasicDBObject) dbObject;
            BasicDBObject keyObj = (BasicDBObject) dbo.get("key");
            List<String> components = new ArrayList<>();
            for (Map.Entry<String, Object> e : keyObj.entrySet()) {
                String val = e.getValue().toString();
                String mongoIndexNameStr = e.getKey();
                String entityFieldName = getJavaFieldNames(entityClazz, mongoIndexNameStr);
                if (entityFieldName == null) {
                    LOGGER.error("Cannot find the mapping from MongoDB index '" + mongoIndexNameStr + "' to Java entities, skipping...");
                    continue;
                }
                Integer intVal;
                if ("hashed".equals(val)) {
                    intVal = 1;
                } else {
                    try {
                        intVal = Double.valueOf(val).intValue();
                    } catch (NumberFormatException e1) {
                        LOGGER.error("Unrecognized index: " + mongoIndexNameStr + "->" + e.getValue());
                        continue;
                    }
                }
                switch (intVal) {
                    case -1:
                        components.add("-" + entityFieldName);
                        break;
                    case 1:
                        components.add(entityFieldName);
                        break;
                    default:
                        LOGGER.warn("Unrecognized index: " + mongoIndexNameStr + "->" + e.getValue());
                        break;
                }
            }
            String morphiaIndex = Joiner.on(",").join(components);
            LOGGER.info("Mapped MongoDB index for " + entityClazz.getName() + " '" + keyObj.toString() + "' to '" + morphiaIndex + "'  ");

            indexStrings.add(morphiaIndex);
        }
        computeMaps(indexStrings);
    }

    private String getJavaFieldNames(Class<?> entityClazz, String mongoIndexName) {
        List<String> comps = Splitter.on('.').splitToList(mongoIndexName);
        List<String> javaComps = new ArrayList<>();
        Class<?> currentClazz = entityClazz;
        for (String comp : comps) {
            MappedClass mc = TypeInfoUtil.MAPPER.getMappedClass(currentClazz);
            MappedField mf = mc.getMappedField(comp);
            if (mf == null) {
                return null;
            }
            javaComps.add(mf.getJavaFieldName());
            //noinspection unchecked
            if (mf.getType().isAssignableFrom(List.class)) {
                currentClazz = (Class<?>) mf.getSubType();
            } else {
                currentClazz = mf.getConcreteType();
            }
        }

        return Joiner.on('.').join(javaComps);
    }

    private void computeMaps(List<String> indexStrings) {
        for (String indexString : indexStrings) {
            indexesMap.add(indexString);
            String sanVal = indexString.replace("-", "");

            List<String> split = Splitter.on(",").splitToList(sanVal);
            String acc = split.get(0);

            indexPrefixMap.add(acc);
            for (int i = 1; i < split.size(); i++) {
                acc += "," + split.get(i);
                indexPrefixMap.add(acc);
            }
        }
    }

    public Set<String> getIndexesMap() {
        return indexesMap;
    }

    public Set<String> getIndexPrefixMap() {
        return indexPrefixMap;
    }

}
