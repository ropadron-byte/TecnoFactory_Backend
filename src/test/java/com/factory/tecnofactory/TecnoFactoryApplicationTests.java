package com.factory.tecnofactory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// jwt.secret ya no tiene valor por defecto en application.yaml: el test aporta uno propio
@SpringBootTest(properties = {
		"jwt.secret=clave-solo-para-tests-de-al-menos-32-caracteres",
		"google.client-id=123.apps.googleusercontent.com"
})
class TecnoFactoryApplicationTests {

	@Test
	void contextLoads() {
	}

}
