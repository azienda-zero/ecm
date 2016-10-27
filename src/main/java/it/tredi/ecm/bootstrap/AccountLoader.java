package it.tredi.ecm.bootstrap;

import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import it.tredi.ecm.dao.entity.Role;
import it.tredi.ecm.dao.enumlist.ProfileEnum;
import it.tredi.ecm.dao.enumlist.RoleEnum;
import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Profile;
import it.tredi.ecm.dao.repository.AccountRepository;
import it.tredi.ecm.dao.repository.ProfileRepository;
import it.tredi.ecm.dao.repository.RoleRepository;
import it.tredi.ecm.service.AccountService;

@Component
@org.springframework.context.annotation.Profile({"dev","demo","simone","abarducci"})
public class AccountLoader implements ApplicationListener<ContextRefreshedEvent> {

	private final static Logger LOGGER = LoggerFactory.getLogger(AccountLoader.class);
	private final String defaultDataScadenzaPassword = "2016-12-31";

	private final RoleRepository roleRepository;
	private final AccountService accountService;
	private final AccountRepository accountRepository;
	private final ProfileRepository profileRepository;

	@Autowired
	public AccountLoader(AccountService accountService, AccountRepository accountRepository, RoleRepository roleRepository, ProfileRepository profileRepository) {
		this.accountService= accountService;
		this.accountRepository = accountRepository;
		this.roleRepository = roleRepository;
		this.profileRepository = profileRepository;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		LOGGER.info("BOOTSTRAP ECM - Inizializzazione ACL...");

		Set<Account> accounts = accountRepository.findAll();

		if(accounts.isEmpty()){
			/* ACCREDITAMENTO */
			Role role_readAccreditamento = new Role();
			role_readAccreditamento.setName(RoleEnum.ACCREDITAMENTO_SHOW.name());
			role_readAccreditamento.setDescription("ACCREDITAMENTO (LETTURA)");
			roleRepository.save(role_readAccreditamento);

			Role role_writeAccreditamento = new Role();
			role_writeAccreditamento.setName(RoleEnum.ACCREDITAMENTO_EDIT.name());
			role_writeAccreditamento.setDescription("ACCREDITAMENTO (SCRITTURA)");
			roleRepository.save(role_writeAccreditamento);

			Role role_readAllAccreditamento = new Role();
			role_readAllAccreditamento.setName(RoleEnum.ACCREDITAMENTO_SHOW_ALL.name());
			role_readAllAccreditamento.setDescription("ACCREDITAMENTO (LETTURA TUTTI)");
			roleRepository.save(role_readAllAccreditamento);

			Role role_writeAllAccreditamento = new Role();
			role_writeAllAccreditamento.setName(RoleEnum.ACCREDITAMENTO_EDIT_ALL.name());
			role_writeAllAccreditamento.setDescription("ACCREDITAMENTO (SCRITTURA TUTTI)");
			roleRepository.save(role_writeAllAccreditamento);

			/* PROVIDER */
			Role role_readProvider = new Role();
			role_readProvider.setName(RoleEnum.PROVIDER_SHOW.name());
			role_readProvider.setDescription("PROVIDER (LETTURA)");
			roleRepository.save(role_readProvider);

			Role role_writeProvider = new Role();
			role_writeProvider.setName(RoleEnum.PROVIDER_EDIT.name());
			role_writeProvider.setDescription("PROVIDER (SCRITTURA)");
			roleRepository.save(role_writeProvider);

			Role role_readAllProvider = new Role();
			role_readAllProvider.setName(RoleEnum.PROVIDER_SHOW_ALL.name());
			role_readAllProvider.setDescription("PROVIDER (LETTURA TUTTI)");
			roleRepository.save(role_readAllProvider);

			Role role_writeAllProvider = new Role();
			role_writeAllProvider.setName(RoleEnum.PROVIDER_EDIT_ALL.name());
			role_writeAllProvider.setDescription("PROVIDER (SCRITTURA TUTTI)");
			roleRepository.save(role_writeAllProvider);

			/* USER */
			Role role_readUser = new Role();
			role_readUser.setName(RoleEnum.USER_SHOW.name());
			role_readUser.setDescription("UTENTI (LETTURA)");
			roleRepository.save(role_readUser);

			Role role_writeUser = new Role();
			role_writeUser.setName(RoleEnum.USER_EDIT.name());
			role_writeUser.setDescription("UTENTI (SCRITTURA)");
			roleRepository.save(role_writeUser);

			Role role_readAllUser = new Role();
			role_readAllUser.setName(RoleEnum.USER_SHOW_ALL.name());
			role_readAllUser.setDescription("UTENTI (LETTURA TUTTI)");
			roleRepository.save(role_readAllUser);

			Role role_writeAllUser = new Role();
			role_writeAllUser.setName(RoleEnum.USER_EDIT_ALL.name());
			role_writeAllUser.setDescription("UTENTI (SCRITTURA TUTTI)");
			roleRepository.save(role_writeAllUser);

			Role role_createUser = new Role();
			role_createUser.setName(RoleEnum.USER_CREATE.name());
			role_createUser.setDescription("UTENTI (CREAZIONE)");
			roleRepository.save(role_createUser);

			/* PROFILE PROVIDER */
			Profile profile_provider = new Profile();
			profile_provider.setProfileEnum(ProfileEnum.PROVIDER);
			profile_provider.getRoles().add(role_readProvider);
			profile_provider.getRoles().add(role_writeProvider);
			profile_provider.getRoles().add(role_readAccreditamento);
			profile_provider.getRoles().add(role_writeAccreditamento);
			profileRepository.save(profile_provider);

			/* PROFILE SEGRETERIA */
			Profile profile_admin = new Profile();
			profile_admin.setProfileEnum(ProfileEnum.SEGRETERIA);

			profile_admin.getRoles().add(role_readAllUser);
			profile_admin.getRoles().add(role_writeAllUser);
			profile_admin.getRoles().add(role_createUser);

			profile_admin.getRoles().add(role_readAllProvider);
			profile_admin.getRoles().add(role_writeAllProvider);

			profile_admin.getRoles().add(role_readAllAccreditamento);
			profile_admin.getRoles().add(role_writeAllAccreditamento);

			profileRepository.save(profile_admin);

			/* PROFILE REFEREE */
			Profile profile_referee = new Profile();
			profile_referee.setProfileEnum(ProfileEnum.REFEREE);

			profile_referee.getRoles().add(role_readAllProvider);
			profile_referee.getRoles().add(role_readAllAccreditamento);

			profileRepository.save(profile_referee);

			/* PROFILE COMMISSIONE */
			Profile profile_commissione = new Profile();
			profile_commissione.setProfileEnum(ProfileEnum.COMMISSIONE);

			profile_commissione.getRoles().add(role_readAllProvider);
			profile_commissione.getRoles().add(role_readAllAccreditamento);

			profileRepository.save(profile_commissione);

			/* PROFILE OSSERVATORE */
			Profile profile_osservatore = new Profile();
			profile_osservatore.setProfileEnum(ProfileEnum.OSSERVATORE);

			profile_osservatore.getRoles().add(role_readAllProvider);
			profile_osservatore.getRoles().add(role_readAllAccreditamento);

			profileRepository.save(profile_osservatore);

			Account provider = new Account();
			provider.setUsername("provider");
			provider.setPassword("$2a$10$JCx8DPs0l0VNFotVGkfW/uRyJzFfc8HkTi5FQy0kpHSpq7W4iP69.");
			//provider.setPassword("admin");
			provider.setEmail("tiommi@3di.it");
			provider.setChangePassword(false);
			provider.setEnabled(true);
			provider.setExpiresDate(null);
			provider.setLocked(false);
			provider.getProfiles().add(profile_provider);
			provider.setDataScadenzaPassword(LocalDate.now());

			try {
				accountService.save(provider);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Account admin = new Account();
			admin.setUsername("segreteria");
			admin.setPassword("$2a$10$JCx8DPs0l0VNFotVGkfW/uRyJzFfc8HkTi5FQy0kpHSpq7W4iP69.");
			//admin.setPassword("admin");
			admin.setEmail("dpranteda@3di.it");
			admin.setChangePassword(false);
			admin.setEnabled(true);
			admin.setExpiresDate(null);
			admin.setLocked(false);
			admin.setNome("Tizio");
			admin.setCognome("Caio");
			admin.getProfiles().add(profile_admin);
			admin.setDataScadenzaPassword(LocalDate.now());

			try {
				accountService.save(admin);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Account referee1 = new Account();
			referee1.setUsername("referee1");
			referee1.setPassword("$2a$10$JCx8DPs0l0VNFotVGkfW/uRyJzFfc8HkTi5FQy0kpHSpq7W4iP69.");
			//referee1.setPassword("admin");
			referee1.setEmail("referee1@ecm.it");
			referee1.setChangePassword(false);
			referee1.setEnabled(true);
			referee1.setExpiresDate(null);
			referee1.setLocked(false);
			referee1.setNome("John");
			referee1.setCognome("Doe");
			referee1.getProfiles().add(profile_referee);
			referee1.setDataScadenzaPassword(LocalDate.parse(defaultDataScadenzaPassword));
			try {
				accountService.save(referee1);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Account referee2 = new Account();
			referee2.setUsername("referee2");
			referee2.setPassword("$2a$10$JCx8DPs0l0VNFotVGkfW/uRyJzFfc8HkTi5FQy0kpHSpq7W4iP69.");
			//referee2.setPassword("admin");
			referee2.setEmail("referee2@ecm.it");
			referee2.setChangePassword(false);
			referee2.setEnabled(true);
			referee2.setExpiresDate(null);
			referee2.setLocked(false);
			referee2.setNome("Mario");
			referee2.setCognome("Rossi");
			referee2.getProfiles().add(profile_referee);
			referee2.setDataScadenzaPassword(LocalDate.parse(defaultDataScadenzaPassword));
			try {
				accountService.save(referee2);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Account referee3 = new Account();
			referee3.setUsername("referee3");
			referee3.setPassword("$2a$10$JCx8DPs0l0VNFotVGkfW/uRyJzFfc8HkTi5FQy0kpHSpq7W4iP69.");
			//referee3.setPassword("admin");
			referee3.setEmail("referee3@ecm.it");
			referee3.setChangePassword(false);
			referee3.setEnabled(true);
			referee3.setExpiresDate(null);
			referee3.setLocked(false);
			referee3.setNome("Marco");
			referee3.setCognome("Marroni");
			referee3.getProfiles().add(profile_referee);
			referee3.setDataScadenzaPassword(LocalDate.parse(defaultDataScadenzaPassword));
			try {
				accountService.save(referee3);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			LOGGER.info("BOOTSTRAP ECM - ACL creata");
		}else{
			LOGGER.info("BOOTSTRAP ECM - ACL trovata (" + accounts.size() + ")");
		}
	}
}
