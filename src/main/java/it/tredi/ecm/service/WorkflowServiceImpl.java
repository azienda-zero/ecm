package it.tredi.ecm.service;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.omg.CosNaming.BindingIteratorPOA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.tredi.bonita.api.IBonitaAPIWrapper;
import it.tredi.bonita.api.model.ActivityDataModel;
import it.tredi.bonita.api.model.ProcessDefinitionDataModel;
import it.tredi.bonita.api.model.ProcessInstanceDataModel;
import it.tredi.bonita.api.model.TaskInstanceDataModel;
import it.tredi.bonita.api.model.UserDataModel;
import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.Profile;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.WorkflowInfo;
import it.tredi.ecm.dao.enumlist.AccreditamentoStatoEnum;
import it.tredi.ecm.dao.enumlist.WorkflowTipoEnum;
import it.tredi.ecm.dao.repository.AccountRepository;
import it.tredi.ecm.dao.repository.AccreditamentoRepository;
import it.tredi.ecm.service.bean.CurrentUser;
import it.tredi.ecm.service.bean.ProcessInstanceDataModelComplete;
import it.tredi.ecm.utils.Utils;

@Service
public class WorkflowServiceImpl implements WorkflowService {
	private static Logger LOGGER = LoggerFactory.getLogger(WorkflowServiceImpl.class);

	final String PROFILE_USER = "User";
	final String PROFILE_ADMINISTRATOR = "Administrator";
	final String ROLE_MEMBER = "Membro";

	@Value("${bonita.admin.username}")
	private String adminUsername;

	@Value("${bonita.admin.password}")
	private String adminPassword;

	@Value("${bonita.users.password}")
	private String usersPassword;

	@Value("${bonita.bonitaviewserverurl}")
	private String bonitaViewServerUrl;

	@Value("${bonita.createaccountonlogin}")
	private boolean createAccountOnLogin;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private AccreditamentoRepository accreditamentoRepository;

	@Autowired
	private IBonitaAPIWrapper bonitaAPIWrapper;

	public WorkflowServiceImpl() {
		//do nothing
		LOGGER.info("WorkflowService constructor");
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getUserPassword() {
		return usersPassword;
	}

	public void setUserPassword(String usersPassword) {
		this.usersPassword = usersPassword;
	}

	public String getBonitaViewServerUrl() {
		return bonitaViewServerUrl;
	}

	public void setBonitaViewServerUrl(String bonitaViewServerUrl) {
		this.bonitaViewServerUrl = bonitaViewServerUrl;
	}

	@Transactional
	public void saveOrUpdateBonitaUserByAccount(Account account) throws Exception {
		Map<String, String> userGroups = new HashMap<String, String>();
		//Aggiungo il gruppo tutti
		userGroups.put("tutti", "Tutti");
		for(Profile profile : account.getProfiles()){
			userGroups.put(profile.getProfileEnum().name(), profile.getName());
		}
		if(account.getUsernameWorkflow() == null || account.getUsernameWorkflow().isEmpty()) {
			//devo creare l'utente
			String userName = account.getUsername();
			String nome = account.getNome();
			if(nome == null || nome.isEmpty())
				nome = userName;
			String cognome = account.getCognome();
			if(cognome == null || cognome.isEmpty())
				cognome = userName;
			else
				cognome += " (" + userName + ")";
			try {
				UserDataModel user = bonitaAPIWrapper.createUserOfUserProfile(userName, usersPassword, nome, cognome, userGroups);
				account.setUsernameWorkflow(userName);
				accountRepository.save(account);
				bonitaAPIWrapper.updateActorsOfAllUserTask();
			} catch (Exception e) {
				//potrebbe essere possibile che in test l'utente esista gia' e sia stato cancellato dal db
				try {
					UserDataModel userDataModel = bonitaAPIWrapper.getUserByLogin(userName);
					if(userDataModel != null) {
						bonitaAPIWrapper.updateUserOfUserProfile(userDataModel.getId(), userGroups);
						account.setUsernameWorkflow(userName);
						accountRepository.save(account);
						bonitaAPIWrapper.updateActorsOfAllUserTask();
						LOGGER.warn("L'utente userName: " + userName + " esiste già su bonita anche se account.getUsernameworkflow() non è impostato. Lo aggiorno");
					} else {
						LOGGER.error("Impossibile creare l'utente userName: " + userName + " su bonita", e);
						throw e;						
					}
				} catch (Exception ex) {
					LOGGER.error("Impossibile creare o aggiornare l'utente userName: " + userName + " su bonita", e);
					LOGGER.error("Impossibile creare o aggiornare l'utente userName: " + userName + " su bonita", ex);
					throw ex;
				}
			}
		} else {
			//devo controllare se l'utente è stato modificato ed eventualmente aggiornarlo
			try {
				UserDataModel userDataModel = bonitaAPIWrapper.getUserByLogin(account.getUsernameWorkflow());
				if(userDataModel != null) {
					bonitaAPIWrapper.updateUserOfUserProfile(userDataModel.getId(), userGroups);
					bonitaAPIWrapper.updateActorsOfAllUserTask();
				} else {
					LOGGER.error("Impossibile trovare l'utente userName: " + account.getUsernameWorkflow() + " su bonita");
					throw new Exception("Impossibile trovare l'utente userNameworkflow: " + account.getUsernameWorkflow() + " su bonita");
				}
			} catch (Exception ex) {
				LOGGER.error("Impossibile aggiornare l'utente userNameworkflow: " + account.getUsernameWorkflow() + " su bonita", ex);
				throw ex;
			}
		}
	}

	//TODO creare una entity che contenga contemporaneamente i dati seguenti
	//ProcessInstanceDataModel, List<ActivityDataModel>, List<TaskInstanceDataModel>
	public ProcessInstanceDataModel getProcessInstance(long processInstanceId) throws Exception {
		return bonitaAPIWrapper.getProcessInstance(processInstanceId);
	}

	public List<ActivityDataModel> getProcessInstanceActivities(long processInstanceId) throws Exception {
		return bonitaAPIWrapper.getProcessInstanceActivities(processInstanceId);
	}

	public List<TaskInstanceDataModel> getUserTasksList(UserDataModel user, long processInstanceId) throws Exception {
		return bonitaAPIWrapper.getUserTasksList(user, processInstanceId);
	}

	public ProcessInstanceDataModelComplete getProcessInstanceDataModelComplete(long processInstanceId, UserDataModel user) throws Exception {
		ProcessInstanceDataModelComplete processInstanceDataModelComplete = null;
		ProcessInstanceDataModel processInstanceDataModel = this.getProcessInstance(processInstanceId);
		if(processInstanceDataModel != null) {
			processInstanceDataModelComplete = new ProcessInstanceDataModelComplete();
			processInstanceDataModelComplete.setProcessInstanceDataModel(processInstanceDataModel);
			processInstanceDataModelComplete.setActivitieDataModels(this.getProcessInstanceActivities(processInstanceId));
			processInstanceDataModelComplete.setTaskInstanceDataModels(this.getUserTasksList(user, processInstanceId));
		}
		return processInstanceDataModelComplete;
	}

	public UserDataModel getUserByLogin(String userName) throws Exception {
		return bonitaAPIWrapper.getUserByLogin(userName);
	}

	public boolean isCreateAccountOnLogin() {
		return createAccountOnLogin;
	}

	//TODO controllare se serve
	/*
	public WorkflowInfo createWorkflow(CurrentUser user, WorkflowTipoEnum workflowTipoEnum) throws Exception {
		//Carico il processDefinition tramite processDefinitionName
		WorkflowInfo workflowInfo = null;
		List<ProcessDefinitionDataModel> listProc = bonitaAPIWrapper.getProcessDefinitionCanBeStartedByUser(user.getWorkflowUserDataModel(), workflowTipoEnum.name(), null);
		if(listProc.size() != 1)
			throw new Exception("Torvati " + listProc.size() + " process definition invece di 1.");
		return workflowInfo;
	}
	 */

	public WorkflowInfo createWorkflowAccreditamentoProvvisorio(CurrentUser user, Accreditamento accreditamento) throws Exception {
		//Carico il processDefinition tramite processDefinitionName
		WorkflowInfo workflowInfo = null;
		List<ProcessDefinitionDataModel> listProc = bonitaAPIWrapper.getProcessDefinitionCanBeStartedByUser(user.getWorkflowUserDataModel(), WorkflowTipoEnum.ACCREDITAMENTOPROVVISORIO.getNome(), null);
		if(listProc.size() != 1)
			throw new Exception("Trovati " + listProc.size() + " process definition con nome " + WorkflowTipoEnum.ACCREDITAMENTOPROVVISORIO.name() + " invece di 1.");
		ProcessDefinitionDataModel processDefinition = listProc.get(0);
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("idProvider", accreditamento.getProvider().getId());
		variables.put("idAccreditamento", accreditamento.getId());

		long processDefinitionId = bonitaAPIWrapper.getProcessDefinitionId(processDefinition.getName(), processDefinition.getVersion());

		ProcessInstanceDataModel processInstanceDataModel = bonitaAPIWrapper.startProcessInstance(processDefinitionId, user.getWorkflowUserDataModel(), variables);
		WorkflowInfo workFlowInfo = new WorkflowInfo();
		workFlowInfo.setDataAvvio(LocalDate.now());
		workFlowInfo.setProcessDefinitionId(processDefinitionId);
		workFlowInfo.setProcessDefinitionName(processDefinition.getName());
		workFlowInfo.setProcessDefinitionVersion(processDefinition.getVersion());
		workFlowInfo.setProcessInstanceId(processInstanceDataModel.getId());
		accreditamento.setWorkflowInfoAccreditamento(workFlowInfo);
		accreditamentoRepository.save(accreditamento);
		return workflowInfo;
	}

	public List<AccreditamentoStatoEnum> getInserimentoEsitoOdgStatiPossibiliAccreditamento(long processInstanceId) throws Exception {
		List<String> stati = new ArrayList<String>();
		Serializable val = bonitaAPIWrapper.getProcessVariable(processInstanceId, "statiPossibiliEsitoODG");
		if(val != null) {
			stati = (List<String>)val;
		}
		
		List<AccreditamentoStatoEnum> result = new ArrayList<AccreditamentoStatoEnum>();
		stati.forEach(s -> result.add(AccreditamentoStatoEnum.valueOf(s)));
		
		return result;
	}

	public void eseguiTaskValutazioneAssegnazioneCrecmForCurrentUser(Accreditamento accreditamento, List<String> usernameWorkflowValutatoriCrecm, Integer numeroValutazioniCrecmRichieste) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskValutazioneAssegnazioneCrecmForUser(user, accreditamento, usernameWorkflowValutatoriCrecm, numeroValutazioniCrecmRichieste);
	}

	public void eseguiTaskValutazioneAssegnazioneCrecmForUser(CurrentUser user, Accreditamento accreditamento, List<String> usernameWorkflowValutatoriCrecm, Integer numeroValutazioniCrecmRichieste) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA_ASSEGNAMENTO) {
			LOGGER.error("Non è possibile eseguire il task Valutazione Assegnazione Crecm per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Valutazione Assegnazione Crecm per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Valutazione Assegnazione Crecm non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Valutazione Assegnazione Crecm non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.setTaskVariable(task.getId(), "usernameListValutazioneCrecm", (Serializable)usernameWorkflowValutatoriCrecm);
		bonitaAPIWrapper.setTaskVariable(task.getId(), "numeroValutazioniCrecmRichieste", (Serializable)numeroValutazioniCrecmRichieste);
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}

	public void eseguiTaskValutazioneCrecmForCurrentUser(Accreditamento accreditamento) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskValutazioneCrecmForUser(user, accreditamento);
	}

	public void eseguiTaskValutazioneCrecmForUser(CurrentUser user, Accreditamento accreditamento) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.VALUTAZIONE_CRECM) {
			LOGGER.error("Non è possibile eseguire il task Valutazione Crecm per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Valutazione Crecm per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Valutazione Crecm non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Valutazione Crecm non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}

	public TaskInstanceDataModel currentUserGetTaskForState(Accreditamento accreditamento) throws Exception {
		return userGetTaskForState(Utils.getAuthenticatedUser(), accreditamento);
	}

	public TaskInstanceDataModel userGetTaskForState(CurrentUser user, Accreditamento accreditamento) throws Exception {
		if(accreditamento.getWorkflowInfoAccreditamento() == null || accreditamento.getWorkflowInfoAccreditamento().getProcessInstanceId() == null)
			return null;
		AccreditamentoStatoEnum stato = accreditamento.getStato();
		List<TaskInstanceDataModel> tasks = bonitaAPIWrapper.getUserTasksList(user.getWorkflowUserDataModel(), accreditamento.getWorkflowInfoAccreditamento().getProcessInstanceId().longValue());
		for(TaskInstanceDataModel task : tasks) {
			if(stato.name().equals(task.getDescription()))
				return task;
		}
		return null;
	}

	/*
	// FORSE NON SERVIRA'
	public void setCookie(ExternalContext context, CookieBonita restCookie) {
		HttpServletResponse response = (HttpServletResponse)context.getNativeResponse();
		Cookie cookie = new Cookie(restCookie.getName(), restCookie.getValue());
		cookie.setPath(restCookie.getPath());
		if(restCookie.getDomain() != null)
			cookie.setDomain(restCookie.getDomain());
		response.addCookie(cookie);
	}
	 */


	public void eseguiTaskInsOdgForCurrentUser(Accreditamento accreditamento) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskInsOdgForUser(user, accreditamento);
	};



	public void eseguiTaskInsOdgForUser(CurrentUser user, Accreditamento accreditamento) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.INS_ODG) {
			LOGGER.error("Non è possibile eseguire il task INS_ODG per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task INS_ODG per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task INS_ODGm non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task INS_ODG non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}

	@Override
	public void prendiTaskInCarica(CurrentUser user, Accreditamento accreditamento) throws Exception{
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task " + task.getName() + " non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task " + task.getName() + " non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
	}
	
	public void eseguiTaskInserimentoEsitoOdgForCurrentUser(Accreditamento accreditamento, AccreditamentoStatoEnum stato) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskTaskInserimentoEsitoOdgForUser(user, accreditamento, stato);
	}
	
	public void eseguiTaskTaskInserimentoEsitoOdgForUser(CurrentUser user, Accreditamento accreditamento, AccreditamentoStatoEnum stato) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.VALUTAZIONE_COMMISSIONE) {
			LOGGER.error("Non è possibile eseguire il task Inserimento Esito ODG per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Inserimento Esito ODG per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Inserimento Esito ODG non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Inserimento Esito ODG non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.setTaskVariable(task.getId(), "stato", (Serializable)(stato.name()));
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}

	public void eseguiTaskAssegnazioneCrecmForCurrentUser(Accreditamento accreditamento, List<String> usernameWorkflowValutatoriCrecm, Integer numeroValutazioniCrecmRichieste) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskAssegnazioneCrecmForUser(user, accreditamento, usernameWorkflowValutatoriCrecm, numeroValutazioniCrecmRichieste);
	}
	
	public void eseguiTaskAssegnazioneCrecmForUser(CurrentUser user, Accreditamento accreditamento, List<String> usernameWorkflowValutatoriCrecm, Integer numeroValutazioniCrecmRichieste) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.ASSEGNAMENTO) {
			LOGGER.error("Non è possibile eseguire il task Assegnazione Crecm per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Assegnazione Crecm per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Assegnazione Crecm non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Assegnazione Crecm non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.setTaskVariable(task.getId(), "usernameListValutazioneCrecm", (Serializable)usernameWorkflowValutatoriCrecm);
		bonitaAPIWrapper.setTaskVariable(task.getId(), "numeroValutazioniCrecmRichieste", (Serializable)numeroValutazioniCrecmRichieste);
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}
	
	public void eseguiTaskRichiestaIntegrazioneForCurrentUser(Accreditamento accreditamento, Long timerIntegrazioneRigetto) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskRichiestaIntegrazioneForUser(user, accreditamento, timerIntegrazioneRigetto);
	}
	
	public void eseguiTaskRichiestaIntegrazioneForUser(CurrentUser user, Accreditamento accreditamento, Long timerIntegrazioneRigetto) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE) {
			LOGGER.error("Non è possibile eseguire il task Richiesta Integrazione per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Richiesta Integrazione per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Richiesta Integrazione non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Richiesta Integrazione non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.setTaskVariable(task.getId(), "timerIntegrazioneRigetto", (Serializable)timerIntegrazioneRigetto);
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}
	
	public void eseguiTaskIntegrazioneForCurrentUser(Accreditamento accreditamento) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskIntegrazioneForUser(user, accreditamento);
	}
	
	public void eseguiTaskIntegrazioneForUser(CurrentUser user, Accreditamento accreditamento) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.INTEGRAZIONE) {
			LOGGER.error("Non è possibile eseguire il task Integrazione per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Integrazione per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Integrazione non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Integrazione non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}
	
	public void eseguiTaskValutazioneSegreteriaForCurrentUser(Accreditamento accreditamento, Boolean presaVisione) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskValutazioneSegreteriaForUser(user, accreditamento, presaVisione);
	}
	
	public void eseguiTaskValutazioneSegreteriaForUser(CurrentUser user, Accreditamento accreditamento, Boolean presaVisione) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA) {
			LOGGER.error("Non è possibile eseguire il task ValutazioneSegreteria per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task ValutazioneSegreteria per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task ValutazioneSegreteria non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task ValutazioneSegreteria non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.setTaskVariable(task.getId(), "presaVisione", (Serializable)presaVisione);
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}
	
	public void eseguiTaskRichiestaPreavvisoRigettoForCurrentUser(Accreditamento accreditamento, Long timerIntegrazioneRigetto) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskRichiestaPreavvisoRigettoForUser(user, accreditamento, timerIntegrazioneRigetto);
	}
	
	public void eseguiTaskRichiestaPreavvisoRigettoForUser(CurrentUser user, Accreditamento accreditamento, Long timerIntegrazioneRigetto) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.RICHIESTA_PREAVVISO_RIGETTO) {
			LOGGER.error("Non è possibile eseguire il task Richiesta Preavviso di Rigetto per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Richiesta Preavviso di Rigetto per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Richiesta Preavviso di Rigetto non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Richiesta Preavviso di Rigetto non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.setTaskVariable(task.getId(), "timerIntegrazioneRigetto", (Serializable)timerIntegrazioneRigetto);
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}

	public void eseguiTaskPreavvisoRigettoForCurrentUser(Accreditamento accreditamento) throws Exception {
		//Ricavo l'utente corrente
		CurrentUser user = Utils.getAuthenticatedUser();
		eseguiTaskPreavvisoRigettoForUser(user, accreditamento);
	}
	
	public void eseguiTaskPreavvisoRigettoForUser(CurrentUser user, Accreditamento accreditamento) throws Exception {
		if(accreditamento.getStato() != AccreditamentoStatoEnum.PREAVVISO_RIGETTO) {
			LOGGER.error("Non è possibile eseguire il task Preavviso Rigetto per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());
			throw new Exception("Non è possibile eseguire il task Preavviso Rigetto per un accreditamento non nello stato corretto - Accreditamento.stato: " + accreditamento.getStato());			
		}
		TaskInstanceDataModel task = userGetTaskForState(user, accreditamento);
		if(task == null) {
			LOGGER.error("Il task Preavviso Rigetto non è disponibile per l'utente username: " + user.getUsername() + " - Accreditamento: " + accreditamento.getId());
			throw new Exception("Il task Preavviso Rigetto non è disponibile per l'utente username: " + user.getUsername());
		}
		if(!task.isAssigned()) {
			//lo prendo in carico
			bonitaAPIWrapper.assignTask(user.getWorkflowUserDataModel(), task.getId());
		}
		bonitaAPIWrapper.executeTask(user.getWorkflowUserDataModel(), task.getId());
	}
	

}