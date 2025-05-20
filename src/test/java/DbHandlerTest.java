import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.tests.DatabaseTestSuite;
import net.berndreiss.zentodo.persistence.DbHandler;
import org.junit.BeforeClass;

public class DbHandlerTest extends DatabaseTestSuite {
    @BeforeClass
    public static void initDB() {
            DatabaseTestSuite.databaseSupplier =
                    () -> {
                        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ZenToDoPU");
                        return new DbHandler(emf, null);
                    };
    }

}
