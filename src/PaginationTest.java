import org.junit.Assert;
import org.junit.Test;

public class PaginationTest {
	private static int totalCount = 211;
	private static int pageSize = 100;

	@Test
	public void testNext() {
		Assert.assertTrue(getNextPage(1));
		Assert.assertTrue(getNextPage(2));
		Assert.assertFalse(getNextPage(3));
	}

	private static boolean getNextPage(int currentPage) {
		boolean nextPage = false;
		int totalPages = (totalCount / pageSize);
		int lastPage = (totalCount % pageSize);
		if (lastPage > 0) {
			totalPages++;
		}
		if (currentPage < totalPages) {
			nextPage = true;
		}
		return nextPage;
	}
}
