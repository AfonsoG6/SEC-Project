package pt.tecnico.sec.bftb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import pt.tecnico.sec.bftb.client.Client;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairGenerationFailedException;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairLoadingFailedException;

import java.security.cert.CertificateException;

public class BaseIT {
	static Client client;

	@BeforeAll
	static void oneTimeSetup() {

	}

	@AfterAll
	static void oneTimeCleanup() {

	}

	@BeforeEach
	public void eachSetup()
			throws KeyPairLoadingFailedException, KeyPairGenerationFailedException, CertificateException {
		client = new Client("localhost",29292, 10);
	}

	@AfterEach
	public void eachCleanup() {

	}
}
