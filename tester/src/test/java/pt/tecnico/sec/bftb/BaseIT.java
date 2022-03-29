package pt.tecnico.sec.bftb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import pt.tecnico.sec.bftb.client.Client;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairGenerationFailedException;
import pt.tecnico.sec.bftb.client.exceptions.KeyPairLoadingFailedException;

import java.io.IOException;
import java.security.cert.CertificateException;

public class BaseIT {

	protected static Client client;

	@BeforeAll
	public static void oneTimeSetup () {
		try {
			Process server = Runtime.getRuntime().exec("mvn exec:java -pl server");
			client = new Client("localhost:44444");
		} catch (IOException e) {
			// TODO handle exception
		}
		catch (KeyPairLoadingFailedException | KeyPairGenerationFailedException | CertificateException e) {
			// TODO handle exception
		}
	}

	@AfterAll
	public static void oneTimeCleanup() {

	}

	@BeforeEach
	public static void eachSetup() {

	}

	@AfterEach
	public static void eachCleanup() {

	}
}
