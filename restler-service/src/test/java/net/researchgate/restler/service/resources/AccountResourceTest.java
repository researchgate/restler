package net.researchgate.restler.service.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.dropwizard.jersey.guava.OptionalMessageBodyWriter;
import io.dropwizard.jersey.guava.OptionalParamFeature;
import io.dropwizard.testing.junit.ResourceTestRule;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.PatchContext;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.results.EntityList;
import net.researchgate.restdsl.results.EntityMultimap;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restler.domain.Account;
import net.researchgate.restler.domain.AccountState;
import net.researchgate.restler.domain.AccountStats;
import net.researchgate.restler.domain.Publication;
import net.researchgate.restler.service.exceptions.ServiceException;
import net.researchgate.restler.service.exceptions.ServiceExceptionMapper;
import net.researchgate.restler.service.model.AccountModel;
import net.researchgate.restler.service.model.PublicationModel;
import net.researchgate.restler.service.modules.TestRestlerModule;
import net.researchgate.restler.service.util.AbstractMongoDBTest;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Account resource test
 */

public class AccountResourceTest extends AbstractMongoDBTest {

    @Inject
    private Datastore datastore;

    @Inject
    private PublicationModel publicationModel;

    @Inject
    private AccountModel accountModel;

    private static TestRestlerModule testRestlerModule = new TestRestlerModule();
    private static Injector injector = Guice.createInjector(testRestlerModule);

    @Rule
    public ResourceTestRule resources = getResources();

    private ResourceTestRule getResources() {
        return ResourceTestRule.builder()
                .addResource(injector.getInstance(AccountResource.class))
                .addProvider(OptionalMessageBodyWriter.class)
                .addProvider(OptionalParamFeature.class)
                .addProvider(new ServiceExceptionMapper())
                .setRegisterDefaultExceptionMappers(false)
                .build();
    }

    private static Random rnd = new Random();

    @BeforeClass
    public static void setUpDB() {
        resetDb();
    }

    @Before
    public void setUpTest() {
        resetDb();
        injector.injectMembers(this);
    }

    private static void resetDb() {
        Datastore ds = injector.getInstance(Datastore.class);
        ds.getDB().dropDatabase();
        injector.getInstance(Morphia.class).mapPackageFromClass(Account.class);
        ds.ensureIndexes();
    }

    @Test
    public void testBasic() {
        Response response = resources.client().target("/accounts/-;").request().get();

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(0);

        response = postAccount(mockedAccount());
        Account savedAccount = response.readEntity(Account.class);
        assertNotNull(savedAccount.getCreatedAt());
        assertNotNull(savedAccount.getModifiedAt());
        assertNotNull(savedAccount.getId());

        response = resources.client().target("/accounts/" + savedAccount.getId()).request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(1);

        resources.client().target("/accounts/-;id <=" + savedAccount.getId()).request().get();
    }


    @Test
    public void testGetById_trailingSlash_accept() {
        assertThat(resources.client().target("/accounts/-;")
                .request().get()
                .readEntity(EntityResult.class).getTotalItems()).isEqualTo(0);


        Account account = new Account();
        account.setNickname("John Doe");
        Account persisted = resources.client().target("/accounts").request()
                .post(Entity.entity(account, MediaType.APPLICATION_JSON_TYPE))
                .readEntity(Account.class);
        assertNotNull(persisted);
        assertNotNull(persisted.getId());


        // retrieve without trailing slash
        assertEquals(1L, resources.client()
                .target("/accounts").path(persisted.getId().toString())
                .request().get()
                .readEntity(EntityResult.class)
                .getTotalItems().longValue());

        // retrieve withtrailing slash
        assertEquals(1L, resources.client()
                .target("/accounts").path(persisted.getId().toString() + "/")
                .request().get()
                .readEntity(EntityResult.class)
                .getTotalItems().longValue());
    }


    @Test
    public void testCountOnly() {
        Response response = postAccount(mockedAccount());
        Account savedAccount = response.readEntity(Account.class);
        response = resources.client().target("/accounts/-;id=" + savedAccount.getId() + ";?limit=0").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        EntityResult entityResult = response.readEntity(EntityResult.class);
        assertThat(entityResult.getTotalItems()).isEqualTo(1);
        assertThat(entityResult.getList().getItems().isEmpty()).isTrue();

        response = resources.client().target(
                "/accounts/-;id=" + savedAccount.getId() + ";?limit=0&groupBy=id").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        entityResult = response.readEntity(EntityResult.class);
        assertThat(entityResult.getTotalItems()).isEqualTo(1);
        EntityList subResult = (EntityList) entityResult.getMultimap().getItems().get(savedAccount.getId().toString());
        assertThat(subResult.getTotalItems()).isEqualTo(1);
        assertThat(subResult.getItems().isEmpty()).isTrue();
    }

    @Test
    public void testGroupBy() {
        Response response = resources.client().target("/accounts/-;").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(0);
        Account account = mockedAccount();
        long ratingA = 42L;
        account.setRating(ratingA);
        response = postAccount(account);
        long ratingB = 43L;
        account.setRating(ratingB);
        response = postAccount(account);

        response = resources.client()
                .target("/accounts/-;rating=" + ratingA + "," + ratingB + "?groupBy=publicationUids")
                .request().build("GET").invoke();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        EntityResult<Account> groupedResponse = null;
        try {
            groupedResponse = resources.client()
                    .target("/accounts/-;rating=" + ratingA + "," + ratingB + "?groupBy=rating")
                    .request().build("GET").invoke(EntityResult.getGenericType(Account.class));
        } catch (ClientErrorException e) {
            System.out.println("Request error: " + e.getResponse().readEntity(String.class));
            fail();
        }

        assertNotNull(groupedResponse.getMultimap());
        EntityMultimap<Account> mm = groupedResponse.getMultimap();

        for (EntityList<Account> v : mm.getItems().values()) {
            assertEquals(1, (long) v.getTotalItems());
        }


//
//        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
//        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(1);
//
//        resources.client().target("/accounts/-;id <=" + savedAccount.getId()).request().get();
    }

    @Test
    public void testModelCooperation() throws RestDslException, ServiceException {
        Account account = postAccount(mockedAccount()).readEntity(Account.class);

        Publication p = new Publication();
        p.setPublicationUid(1L);
        p.setTitle("First publication");

        publicationModel.save(p);

        EntityResult<Publication> pubs = publicationModel.get(ServiceQuery.all());
        assertEquals(1, pubs.asList().size());

        accountModel.deleteById(account.getId());

        pubs = publicationModel.get(ServiceQuery.all());
        assertEquals(0L, (long) pubs.getTotalItems());

    }

    @Test
    public void testAdvancedOps() throws ServiceException {
        Account account = postAccount(mockedAccount()).readEntity(Account.class);
        Response response;
        EntityResult<Account> res;

        response = resources.client().target("/accounts/-;rating >=" + (account.getRating() - 1)).request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(1, res.getList().getItems().size());

        response = resources.client().target("/accounts/-;rating >=" + (account.getRating())).request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(0, res.getList().getItems().size());

        response = resources.client().target("/accounts/-;rating >%3D=" + (account.getRating())).request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(1, res.getList().getItems().size());

        response = resources.client().target("/accounts/-;rating__gte=" + (account.getRating())).request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(1, res.getList().getItems().size());

        response = resources.client().target("/accounts/-;rating__le=" + (account.getRating())).request().get();
        assertEquals(400, response.getStatus());
    }


    @Test
    public void testSynMatch() throws ServiceException {
        Account account = new Account();
        account.setNickname("pavel");
        account.setRating(1L);

        account.setAdditionalStats(Lists.newArrayList(new AccountStats(1, 1), new AccountStats(2, 2)));
        postAccount(account);

//        account.setNickname("nik");
//        postAccount(account);

        Response response;
        EntityResult<Account> res;

        response = resources.client()
                .target("/accounts/-;rating=1;nickname=$any;additionalStats.publicationCnt=1;additionalStats.followerCnt=2").request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(1, res.getList().getItems().size());

        response = resources.client()
                .target("/accounts/-;rating=1;nickname=$any;additionalStats.publicationCnt=1;additionalStats.followerCnt=2?syncMatch=additionalStats").request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(0, res.getList().getItems().size());

        response = resources.client()
                .target("/accounts/-;rating=1;nickname=$any;additionalStats.publicationCnt=1;additionalStats.followerCnt=1?syncMatch=additionalStats").request().get();
        res = readResponseOrFail(Account.class, response);
        assertEquals(1, res.getList().getItems().size());

    }


    @Test
    public void testPatch() throws RestDslException, ServiceException {
        Account account = postAccount(mockedAccount()).readEntity(Account.class);

        Account newAccount = new Account();
        newAccount.setId(account.getId());

        String newNickname = "einstein";
        List<Long> newPubUids = Arrays.asList(1L, 2L);

        newAccount.setNickname(newNickname);
        newAccount.setPublicationUids(newPubUids);

        Account patchedAccount = getPatchedAccount(newAccount);

        account.setNickname(newNickname);
        account.setModifiedAt(patchedAccount.getModifiedAt());
        account.setPublicationUids(newPubUids);

        assertEquals(account, patchedAccount);

        Account returnedAccount = accountModel.patch(newAccount, PatchContext.DEFAULT_CONTEXT);
        assertEquals(patchedAccount.getId(), returnedAccount.getId());
        assertEquals(account.getNickname(), returnedAccount.getNickname());
        assertEquals(account.getPublicationUids(), returnedAccount.getPublicationUids());

        returnedAccount = accountModel.patch(returnedAccount,
                PatchContext.builder().unsetFields(Sets.newHashSet("publicationUids", "nickname")).build());
        assertEquals(patchedAccount.getId(), returnedAccount.getId());

        assertEquals(null, returnedAccount.getNickname());
        assertEquals(null, returnedAccount.getPublicationUids());
    }

    private Account getPatchedAccount(Account newAccount) {
        return resources.client().target("/accounts")
                .request()
                .header("X-Client-Id", "restler-unit-test")
                .build("PATCH", Entity.entity(newAccount, MediaType.APPLICATION_JSON_TYPE))
                .invoke()
                .readEntity(Account.class);
    }

    @Test
    public void testPopulate() {
        resources.client().target("/accounts/-;").request().get();
        for (int i = 0; i < 40; i++) {
            Response resp = postAccount(randomMockedAccount(i));
            Account newAccount = resp.readEntity(Account.class);
            System.out.println("AccountId: " + newAccount.getId());
        }
    }

    @Test
    public void testCounting() {
        int total = 10;
        for (int i = 0; i < total; i++) {
            Response resp = postAccount(randomMockedAccount(i));
            assertTrue(resp.getStatus() == 201);
        }

        EntityResult<Account> res =
                accountModel.get(ServiceQuery.<ObjectId>builder().limit(0).build());

        assertTrue(res.getTotalItems() == total);
        assertTrue(res.getList().getItems().size() == 0);

        for (int limit = 0; limit < 2 * total; limit++) {
            for (int offset = 0; offset < 2 * total; offset++) {
                res = accountModel.get(ServiceQuery.<ObjectId>builder().limit(limit).offset(offset).build());
                assertTrue("For limit=" + limit + " and offset=" + offset, res.getTotalItems() == total);
                assertTrue(res.getList().getItems().size() <= limit);
            }
        }


    }

    //TODO: global constant for X-Client-id header
    private Response postAccount(Account account) {
        return resources.client().target("/accounts").request().header("X-Client-Id", "restler-unit-test")
                .post(Entity.entity(account, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void testUpdateOperations() throws RestDslException {
        Account account = mockedAccount();
        Response response = postAccount(account);
        account = response.readEntity(Account.class);

        ObjectId mentorId = new ObjectId();
        accountModel.changeMentor(account.getId(), mentorId);
        ObjectId dbMentorId = accountModel.get(account.getId()).asList().get(0).getMentorAccountId();
        assertEquals(mentorId, dbMentorId);
    }

    private static Account mockedAccount() {
        Account account = new Account();
        account.setPublicationUids(Lists.newArrayList(1L));
        account.setRating(42L);
        return account;
    }

    private static Account randomMockedAccount(int i) {
        Account account = new Account();
        account.setPublicationUids(Lists.newArrayList((long) i, (long) i + 1));
        account.setRating((long) (i * 10));
        account.setMentorAccountId(new ObjectId());
        account.setStats(mockedStats(i));
        if (rnd.nextDouble() > 0.15) {
            account.setNickname("accountNickname" + i);
        }

        account.setLongDate(new Date(new Date().getTime() - rnd.nextInt(100000)));

        int asInd = rnd.nextInt(4);
        AccountState as = null;
        if (asInd != 3) {
            as = AccountState.values()[asInd];
        }
        account.setState(as);
        return account;
    }

    private static AccountStats mockedStats(int i) {
        AccountStats s = new AccountStats();
        s.setFollowerCnt(i * 10);
        s.setPublicationCnt(i * 3);
        s.setScoreBreakdown(Arrays.asList((long) i, (long) i + 3));

        return s;
    }

    @Test
    public void testSpecialValues() {
        Response response = resources.client().target("/accounts/-;").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(0);

        Account mockedAccount1 = mockedAccount();
        mockedAccount1.setRating(null);
        postAccount(mockedAccount1);
        Account mockedAccount2 = mockedAccount();
        mockedAccount2.setRating(100L);
        postAccount(mockedAccount2);

        response = resources.client().target("/accounts/-;rating=200").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(0);

        response = resources.client().target("/accounts/-;rating=100").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(1);

        // key=$null
        response = resources.client().target("/accounts/-;rating=$null").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(1);

        // key=$exists
        response = resources.client().target("/accounts/-;rating=$exists").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(1);

        // key=$any
        response = resources.client().target("/accounts/-;rating=$any").request().get();
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        assertThat(response.readEntity(EntityResult.class).getTotalItems()).isEqualTo(2);
    }

    private <T> EntityResult<T> readResponseOrFail(Class<T> clazz, Response response) {
        if (response.getStatus() != 200) {
            fail("Response is not 200");
        }
        return response.readEntity(EntityResult.getGenericType(clazz));
    }
}
