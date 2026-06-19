package cl.fullstack.order_service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderServiceApplicationTests {

	@Test
	void applicationClass_isAvailableWithoutStartingSpringContext() {
		// Arrange
		Class<?> applicationClass = OrderServiceApplication.class;

		// Act
		String simpleName = applicationClass.getSimpleName();

		// Assert
		assertEquals("OrderServiceApplication", simpleName);
	}

}
