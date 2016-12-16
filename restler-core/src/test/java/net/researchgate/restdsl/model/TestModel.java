package net.researchgate.restdsl.model;

import net.researchgate.restdsl.TestEntity;
import net.researchgate.restdsl.dao.PersistentServiceDao;

public class TestModel extends ServiceModel<TestEntity, Long> {

    public TestModel(PersistentServiceDao<TestEntity, Long> serviceDao) {
        super(serviceDao);
    }

}
