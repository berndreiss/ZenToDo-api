import net.berndreiss.zentodo.tests.DatabaseTestSuite;
import net.berndreiss.zentodo.persistence.TestDbHandler;
import org.junit.BeforeClass;

public class TestDbHandlerTest extends DatabaseTestSuite {
    @BeforeClass
    public static void initDB() {
            DatabaseTestSuite.databaseSupplier =
                    () -> {
                        return new TestDbHandler("ZenToDoPU", null);
                    };
    }

}
