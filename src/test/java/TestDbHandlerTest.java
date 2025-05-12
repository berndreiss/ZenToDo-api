import net.berndreiss.zentodo.data.Database;
import net.berndreiss.zentodo.util.DatabaseTest;
import net.berndreiss.zentodo.util.TestDbHandler;

public class TestDbHandlerTest extends DatabaseTest {
    @Override
    protected Database createDatabase() {
        return new TestDbHandler("ZenToDoPU");
    }
}
