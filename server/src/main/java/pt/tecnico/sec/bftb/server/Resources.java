package pt.tecnico.sec.bftb.server;

import pt.tecnico.sec.bftb.server.exceptions.*;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public class Resources {

	private static final String CRYPTO_PATH = "crypto";
	private static final String KEYSTORE_FILENAME = "keystore.jks";
	private static final String KEYSTORE_PWD = "sec2122";

	private static final String STATE_PATH = "state";
	private static final String ACCOUNTS_PATH = Path.of(STATE_PATH, "accounts").toString();
	private static final String TRANSFERS_PATH = Path.of(STATE_PATH, "transfers").toString();

	private Resources() { /* empty */ }

	public static void init() throws DirectoryCreationFailedException {
		createResourceDirectory(STATE_PATH);
		createResourceDirectory(ACCOUNTS_PATH);
		createResourceDirectory(TRANSFERS_PATH);
	}

	private static String getAbsolutePathOfResource(String path) throws URISyntaxException {
		String pathString = Path.of(path).toString();
		URL pathURL = Resources.class.getClassLoader().getResource(pathString);
		assert pathURL != null;
		return Paths.get(pathURL.toURI()).toString();
	}

	public static PrivateKey getPrivateKey() throws PrivateKeyLoadingFailedException {
		try {
			String pathString = Path.of(CRYPTO_PATH, KEYSTORE_FILENAME).toString();
			InputStream keyStream = Resources.class.getClassLoader().getResourceAsStream(pathString);
			assert keyStream != null;
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(keyStream, KEYSTORE_PWD.toCharArray());
			return (PrivateKey) ks.getKey("1", KEYSTORE_PWD.toCharArray());
		}
		catch (KeyStoreException | UnrecoverableKeyException | CertificateException | IOException | NoSuchAlgorithmException e) {
			throw new PrivateKeyLoadingFailedException(e);
		}
	}

	private static void createResourceDirectory(String dirname) throws DirectoryCreationFailedException {
		try {
			URL resource = Resources.class.getClassLoader().getResource(".");
			assert resource != null;
			Path resourcesPath = Paths.get(resource.toURI());
			Path dirPath = resourcesPath.resolve(dirname);
			Files.createDirectories(dirPath);
		}
		catch (URISyntaxException | IOException e) {
			throw new DirectoryCreationFailedException(e);
		}
	}

	private static void keepPreviousState(String pathString) throws KeepPreviousStateFailedException {
		if (Files.exists(Paths.get(pathString))) {
			if (Files.exists(Paths.get(pathString + ".old"))) {
				File oldFile = new File(pathString + ".old");
				boolean deleteSuccessful = oldFile.delete();
				if (!deleteSuccessful) throw new KeepPreviousStateFailedException("Could not delete old backup file");
			}
			File oldFile = new File(pathString);
			boolean renameSuccessful = oldFile.renameTo(new File(pathString + ".old"));
			if (!renameSuccessful) throw new KeepPreviousStateFailedException("Could not rename old backup file");
		}
	}

	public static Account restorePreviousState(Account account) throws RestorePreviousStateFailedException {
		try {
			String pathString = Path.of(getAbsolutePathOfResource(ACCOUNTS_PATH), account.getHash()).toString();
			if (!Files.exists(Paths.get(pathString + ".old")))
				throw new RestorePreviousStateFailedException("No previous state to restore");
			File oldFile = new File(pathString + ".old");
			boolean renameSuccessful = oldFile.renameTo(new File(pathString));
			if (!renameSuccessful)
				throw new RestorePreviousStateFailedException("Unable to rename old backup file in order to restore previous state");
			return loadAccount(new File(pathString));
		}
		catch (URISyntaxException | NoSuchAlgorithmException | AccountLoadingFailedException e) {
			throw new RestorePreviousStateFailedException(e);
		}
	}

	public static Transfer restorePreviousState(Transfer transfer) throws RestorePreviousStateFailedException {
		try {
			String pathString = Path.of(getAbsolutePathOfResource(TRANSFERS_PATH), Long.toString(transfer.getID())).toString();
			if (!Files.exists(Paths.get(pathString + ".old")))
				throw new RestorePreviousStateFailedException("No previous state to restore");
			File oldFile = new File(pathString + ".old");
			boolean renameSuccessful = oldFile.renameTo(new File(pathString));
			if (!renameSuccessful)
				throw new RestorePreviousStateFailedException("Unable to rename old backup file in order to restore previous state");
			return loadTransfer(new File(pathString));
		}
		catch (URISyntaxException | TransferLoadingFailedException e) {
			throw new RestorePreviousStateFailedException(e);
		}
	}

	public static void saveAccount(Account account) throws AccountSavingFailedException {
		try {
			String accountIdentifier = account.getHash();
			String accountsPathString = getAbsolutePathOfResource(ACCOUNTS_PATH);
			String pathString = Path.of(accountsPathString, accountIdentifier).toString();
			keepPreviousState(pathString);
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(pathString)))) {
				oos.writeObject(account);
			}
			System.out.println("Saved account " + accountIdentifier);
		}
		catch (IOException | NoSuchAlgorithmException | URISyntaxException | KeepPreviousStateFailedException e) {
			throw new AccountSavingFailedException(e);
		}
	}

	public static List<Account> loadAccounts() throws AccountLoadingFailedException {
		try {
			File accountsFolder = new File(getAbsolutePathOfResource(ACCOUNTS_PATH));
			File[] accountsFiles = accountsFolder.listFiles();
			assert accountsFiles != null;
			List<Account> accounts = new ArrayList<>();
			for (File accountFile : accountsFiles) {
				if (accountFile.getPath().endsWith(".old"))
					continue;
				accounts.add(loadAccount(accountFile));
			}
			return accounts;
		}
		catch (URISyntaxException e) {
			throw new AccountLoadingFailedException(e);
		}
	}

	private static Account loadAccount(File accountFile) throws AccountLoadingFailedException {
		try {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(accountFile)))) {
				return (Account) ois.readObject();
			}
		}
		catch (IOException | ClassNotFoundException e) {
			throw new AccountLoadingFailedException(e);
		}
	}

	public static void saveTransfer(Transfer transfer) throws TransferSavingFailedException {
		try {
			String transferIdentifier = Long.toString(transfer.getID());
			String transfersPathString = getAbsolutePathOfResource(TRANSFERS_PATH);
			String pathString = Path.of(transfersPathString, transferIdentifier).toString();
			keepPreviousState(pathString);
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(pathString)))) {
				oos.writeObject(transfer);
			}
		}
		catch (IOException | URISyntaxException | KeepPreviousStateFailedException e) {
			throw new TransferSavingFailedException(e);
		}
		System.out.println("Saved transfer " + transfer.getID());
	}

	public static List<Transfer> loadTransfers() throws TransferLoadingFailedException {
		try {
			File transfersFolder = new File(getAbsolutePathOfResource(TRANSFERS_PATH));
			File[] transfersFiles = transfersFolder.listFiles();
			assert transfersFiles != null;
			List<Transfer> transfers = new ArrayList<>();
			for (File transferFile : transfersFiles) {
				if (transferFile.getPath().endsWith(".old"))
					continue;
				transfers.add(loadTransfer(transferFile));
			}
			return transfers;
		}
		catch (URISyntaxException e) {
			throw new TransferLoadingFailedException(e);
		}
	}

	private static Transfer loadTransfer(File transferFile) throws TransferLoadingFailedException {
		try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(transferFile)))) {
			return (Transfer) ois.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			throw new TransferLoadingFailedException(e);
		}
	}
}
