package it.tredi.ecm.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.FieldIntegrazioneAccreditamento;
import it.tredi.ecm.dao.entity.FieldValutazioneAccreditamento;
import it.tredi.ecm.dao.entity.JsonViewModel;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.Sede;
import it.tredi.ecm.dao.entity.Valutazione;
import it.tredi.ecm.dao.entity.WorkflowInfo;
import it.tredi.ecm.dao.enumlist.AccreditamentoStatoEnum;
import it.tredi.ecm.dao.enumlist.AccreditamentoWrapperModeEnum;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import it.tredi.ecm.dao.enumlist.SubSetFieldEnum;
import it.tredi.ecm.dao.enumlist.TipoIntegrazioneEnum;
import it.tredi.ecm.service.AccreditamentoService;
import it.tredi.ecm.service.FieldEditabileAccreditamentoService;
import it.tredi.ecm.service.FieldIntegrazioneAccreditamentoService;
import it.tredi.ecm.service.FieldValutazioneAccreditamentoService;
import it.tredi.ecm.service.IntegrazioneService;
import it.tredi.ecm.service.ProviderService;
import it.tredi.ecm.service.SedeService;
import it.tredi.ecm.service.ValutazioneService;
import it.tredi.ecm.utils.Utils;
import it.tredi.ecm.web.bean.Message;
import it.tredi.ecm.web.bean.PersonaWrapper;
import it.tredi.ecm.web.bean.RichiestaIntegrazioneWrapper;
import it.tredi.ecm.web.bean.SedeWrapper;
import it.tredi.ecm.web.validator.EnableFieldValidator;
import it.tredi.ecm.web.validator.SedeValidator;
import it.tredi.ecm.web.validator.ValutazioneValidator;

@Controller
public class SedeController {
	public static final Logger LOGGER = LoggerFactory.getLogger(SedeController.class);

	private final String EDIT = "sede/sedeEdit";
	private final String SHOW = "sede/sedeShow";
	private final String VALIDATE = "sede/sedeValidate";
	private final String ENABLEFIELD = "sede/sedeEnableField";

	@Autowired private SedeService sedeService;
	@Autowired private SedeValidator sedeValidator;
	@Autowired private ProviderService providerService;

	@Autowired private ValutazioneService valutazioneService;
	@Autowired private ValutazioneValidator valutazioneValidator;
	@Autowired private FieldEditabileAccreditamentoService fieldEditabileAccreditamentoService;
	@Autowired private FieldValutazioneAccreditamentoService fieldValutazioneAccreditamentoService;

	@Autowired private AccreditamentoService accreditamentoService;
	@Autowired private IntegrazioneService integrazioneService;
	@Autowired private FieldIntegrazioneAccreditamentoService fieldIntegrazioneAccreditamentoService;
	@Autowired private ObjectMapper jacksonObjectMapper;

	@Autowired private EnableFieldValidator enableFieldValidator;

	@RequestMapping("/comuni")
	@ResponseBody
	public List<String>getElencoComuni(@RequestParam String provincia){
		HashMap<String, List<String>> elencoComuni = new HashMap<String, List<String>>();

		List<String> provinciaA = new ArrayList<String>();
		provinciaA.add("Venezia");
		provinciaA.add("Mira");

		List<String> provinciaB = new ArrayList<String>();
		provinciaB.add("Padova");
		provinciaB.add("Cittadella");

		List<String> provinciaC = new ArrayList<String>();
		provinciaC.add("Verona");
		provinciaC.add("Nogara");

		elencoComuni.put("Venezia", provinciaA);
		elencoComuni.put("Padova", provinciaB);
		elencoComuni.put("Verona", provinciaC);

		return elencoComuni.get(provincia);
	}

	@RequestMapping("/cap")
	@ResponseBody
	public List<String>getElencoCap(@RequestParam String comune){
		HashMap<String, List<String>> elencoCap = new HashMap<String, List<String>>();

		List<String> capVenezia = new ArrayList<String>();
		capVenezia.add("30121");
		capVenezia.add("30150");
		capVenezia.add("30176");

		List<String> capMira = new ArrayList<String>();
		capMira.add("30034");


		List<String> capPadova = new ArrayList<String>();
		capPadova.add("35121");
		capPadova.add("35131");
		capPadova.add("35143");

		List<String> capCittadella = new ArrayList<String>();
		capCittadella.add("35013");

		List<String> capVerona = new ArrayList<String>();
		capVerona.add("37121");
		capVerona.add("37131");
		capVerona.add("37142");

		List<String> capNogara = new ArrayList<String>();
		capNogara.add("37054");

		elencoCap.put("Venezia", capVenezia);
		elencoCap.put("Mira", capMira);
		elencoCap.put("Padova", capPadova);
		elencoCap.put("Cittadella", capCittadella);
		elencoCap.put("Verona", capVerona);
		elencoCap.put("Nogara", capNogara);

		return elencoCap.get(comune);
	}

	@ModelAttribute("sedeWrapper")
	public SedeWrapper getSede(@RequestParam(name = "editId", required = false) Long id,
			@RequestParam(value="statoAccreditamento",required = false) AccreditamentoStatoEnum statoAccreditamento,
			@RequestParam(value="accreditamentoId",required = false) Long accreditamentoId,
			@RequestParam(value="wrapperMode",required = false) AccreditamentoWrapperModeEnum wrapperMode) throws Exception{
		if(id != null){
			//return prepareSedeWrapperEdit(sedeService.getSede(id), statoAccreditamento);
			return prepareWrapperForReloadByEditId(sedeService.getSede(id), accreditamentoId, statoAccreditamento, wrapperMode);
		}
		return new SedeWrapper();
	}

	private SedeWrapper prepareWrapperForReloadByEditId(Sede sede, Long accreditamentoId, AccreditamentoStatoEnum statoAccreditamento,
			AccreditamentoWrapperModeEnum wrapperMode) throws Exception{
		if(wrapperMode == AccreditamentoWrapperModeEnum.EDIT)
			return prepareSedeWrapperEdit(sede, accreditamentoId, sede.getProvider().getId(), statoAccreditamento, true);
		if(wrapperMode == AccreditamentoWrapperModeEnum.VALIDATE)
			return prepareSedeWrapperValidate(sede, accreditamentoId, sede.getProvider().getId(), statoAccreditamento,false);

		return new SedeWrapper();
	}

	/***	NEW / EDIT 	***/
	@PreAuthorize("@securityAccessServiceImpl.canEditAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canEditProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/add")
	public String getNewSedeCurrentProvider(@PathVariable Long accreditamentoId, @PathVariable Long providerId,
			Model model, RedirectAttributes redirectAttrs) throws Exception{
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/add"));
		try {
			return goToEdit(model, prepareSedeWrapperEdit(new Sede(), accreditamentoId, providerId, accreditamentoService.getStatoAccreditamento(accreditamentoId),false));
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/add"),ex);
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId));
			return "redirect:/accreditamento" + accreditamentoId;
		}
	}

	/*** EDIT SEDE ***/
	@PreAuthorize("@securityAccessServiceImpl.canEditAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canEditProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/edit")
	public String editSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id, Model model){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/edit"));
		try {
			SedeWrapper sedeWrapper = prepareSedeWrapperEdit(sedeService.getSede(id), accreditamentoId, providerId, accreditamentoService.getStatoAccreditamento(accreditamentoId),false);
			return goToEdit(model, sedeWrapper);
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/edit"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
			return EDIT;
		}
	}

	/*** VALUTAZIONE SEDE ***/
	@PreAuthorize("@securityAccessServiceImpl.canValidateAccreditamento(principal,#accreditamentoId,#showRiepilogo)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/validate")
	public String validateSede(@RequestParam(name = "showRiepilogo", required = false) Boolean showRiepilogo,
			@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id,
			Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/validate"));
		try {
			//controllo se è possibile modificare la valutazione o meno
			model.addAttribute("canValutaDomanda", accreditamentoService.canUserValutaDomanda(accreditamentoId, Utils.getAuthenticatedUser()));

			//aggiungo flag per controllo su campi modificati all'inserimento di una nuova domanda
			Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
			if(accreditamento.isStandard() && accreditamento.isValutazioneSegreteriaAssegnamento())
				model.addAttribute("checkIfAccreditamentoChanged", true);
			SedeWrapper sedeWrapper = prepareSedeWrapperValidate(sedeService.getSede(id), accreditamentoId, providerId, accreditamentoService.getStatoAccreditamento(accreditamentoId), false);
			return goToValidate(model, sedeWrapper);
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/validate"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("REDIRECT: /accreditamento/" + accreditamentoId + "/validate"));
			return "redirect:/accreditamento/" + accreditamentoId + "/validate";
		}
	}

	/*** ENABLEFIELD SEDE ***/
	@PreAuthorize("@securityAccessServiceImpl.canValidateAccreditamento(principal,#accreditamentoId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/enableField")
	public String enableFieldSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id,
			Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/enableField"));
		try {
			return goToEnableField(model,prepareSedeWrapperEnableField(sedeService.getSede(id), accreditamentoId, providerId));
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/enableField"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + SHOW));
			return "redirect:/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/show";
		}
	}

	/*** SHOW SEDE ***/
	@PreAuthorize("@securityAccessServiceImpl.canShowAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canShowProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/show")
	public String showSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id,
			@RequestParam(value = "from", required =  false) String from, Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/show"));
		try {
			if (from != null) {
				redirectAttrs.addFlashAttribute("mode", from);
				return "redirect:/accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/show";
			}
			SedeWrapper sedeWrapper = prepareSedeWrapperShow(sedeService.getSede(id), accreditamentoId, providerId);
			return goToShow(model, sedeWrapper);
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/show"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + SHOW));
			return SHOW;
		}
	}

	/***	SAVE 	***/
	@RequestMapping(value = "/accreditamento/{accreditamentoId}/provider/{providerId}/sede/save", method = RequestMethod.POST)
	public String saveSede(@ModelAttribute("sedeWrapper") SedeWrapper sedeWrapper, BindingResult result,
			Model model, @PathVariable Long providerId, RedirectAttributes redirectAttrs, @PathVariable Long accreditamentoId){
		LOGGER.info(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/save"));
		try{
			Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
			boolean flagIntegrazione = (accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto() || accreditamento.isModificaDati()) ? true : false;
			sedeValidator.validate(sedeWrapper.getSede(), providerService.getProvider(providerId), result, "sede.", flagIntegrazione);
			if(result.hasErrors()){
				model.addAttribute("message",new Message("message.errore", "message.inserire_campi_required", "error"));
				LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
				return EDIT;
			}else{
				if(accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto() || accreditamento.isModificaDati()) {
					integra(sedeWrapper, false);
				}else{
					saveSede(sedeWrapper, providerService.getProvider(providerId));
				}
				redirectAttrs.addAttribute("accreditamentoId", sedeWrapper.getAccreditamentoId());
				redirectAttrs.addFlashAttribute("message", new Message("message.completato", "message.sede_salvata", "success"));
				LOGGER.info(Utils.getLogMessage("REDIRECT: /accreditamento/" + accreditamentoId + "/edit"));
				return "redirect:/accreditamento/{accreditamentoId}/edit";
			}
		}catch(Exception ex){
			LOGGER.error(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/save"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
			return EDIT;
		}
	}

	@PreAuthorize("@securityAccessServiceImpl.canEditAccreditamento(principal,#accreditamentoId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{sedeId}/delete")
	public String removeSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long sedeId,
			Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId +"/provider/"+ providerId + "/sede/" + sedeId + "/delete"));
		try{
			Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
			Sede sede = sedeService.getSede(sedeId);
			//passa per l'integrazione solo se la sede NON è stata inserita in fase di integrazione e non ancora approvata (dirty = true)
			if((accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto() || accreditamento.isModificaDati())){
				if(sede.isDirty()) {
					//rimozione sede multi-istanza dalla Domanda di Accreditamento e relativi IdEditabili e fieldIntegrazione
					sedeService.delete(sedeId);
					fieldEditabileAccreditamentoService.removeFieldEditabileForAccreditamento(accreditamentoId, sedeId, SubSetFieldEnum.SEDE);
					Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
					AccreditamentoStatoEnum stato = accreditamento.getCurrentStato();
					fieldIntegrazioneAccreditamentoService.removeFieldIntegrazioneByObjectReferenceAndContainer(accreditamentoId, stato, workFlowProcessInstanceId, sede.getId());
				}
				else
					integra(new SedeWrapper(sedeService.getSede(sedeId), accreditamentoId), true);
			}else{
				//rimozione sede multi-istanza dalla Domanda di Accreditamento e relativi IdEditabili e fieldIntegrazione
				sedeService.delete(sedeId);
				fieldEditabileAccreditamentoService.removeFieldEditabileForAccreditamento(accreditamentoId, sedeId, SubSetFieldEnum.SEDE);
			}
			redirectAttrs.addFlashAttribute("message", new Message("message.completato", "message.sede_eliminata", "success"));
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId +"/provider/"+ providerId + "/sede/" + sedeId + "/delete"),ex);
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
		}

		redirectAttrs.addAttribute("accreditamentoId", accreditamentoId);
		LOGGER.info(Utils.getLogMessage("REDIRECT: /accreditamento/" + accreditamentoId + "/edit"));
		return "redirect:/accreditamento/{accreditamentoId}/edit";
	}

	/***	SALVA VALUTAZIONE
	 * @throws Exception ***/
	@RequestMapping(value = "/accreditamento/{accreditamentoId}/provider/{providerId}/sede/validate", method = RequestMethod.POST)
	public String valutaSede(@ModelAttribute("sedeWrapper") SedeWrapper sedeWrapper, BindingResult result,
			Model model, @PathVariable Long providerId, RedirectAttributes redirectAttrs, @PathVariable Long accreditamentoId) throws Exception{
		LOGGER.info(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/validate"));
		try{
			//validazione della sede
			Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
			//validator che ignora i FULL
			if(accreditamento.isStandard() && accreditamento.isValutazioneSegreteriaAssegnamento())
				valutazioneValidator.validateValutazioneStandard(sedeWrapper.getMappa(), result);
			else
				valutazioneValidator.validateValutazione(sedeWrapper.getMappa(), result);
			if(result.hasErrors()){
				model.addAttribute("message",new Message("message.errore", "message.inserire_campi_required", "error"));
				model.addAttribute("canValutaDomanda", accreditamentoService.canUserValutaDomanda(accreditamentoId, Utils.getAuthenticatedUser()));
				LOGGER.info(Utils.getLogMessage("VIEW: " + VALIDATE));
				return VALIDATE;
			}else{

				if(sedeWrapper.getMappa() != null && sedeWrapper.getMappa().containsKey(IdFieldEnum.SEDE__FULL)){
					Boolean esitoFull = sedeWrapper.getMappa().get(IdFieldEnum.SEDE__FULL).getEsito();
					if(esitoFull != null){
						sedeWrapper.getMappa().forEach((k, v) -> {v.setEsito(esitoFull.booleanValue());});
					}
				}

				sedeWrapper.getMappa().forEach((k, v) -> {
					v.setIdField(k);
					v.setAccreditamento(accreditamento);
					v.setObjectReference(sedeWrapper.getSede().getId());
				});

				Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamento.getId(), Utils.getAuthenticatedUser().getAccount().getId());
				Set<FieldValutazioneAccreditamento> values = new HashSet<FieldValutazioneAccreditamento>(fieldValutazioneAccreditamentoService.saveMapList(sedeWrapper.getMappa()));
				valutazione.getValutazioni().addAll(values);
				valutazioneService.save(valutazione);

				redirectAttrs.addAttribute("accreditamentoId", sedeWrapper.getAccreditamentoId());
				redirectAttrs.addFlashAttribute("message", new Message("message.completato", "message.valutazione_salvata", "success"));
				LOGGER.info(Utils.getLogMessage("REDIRECT: /accreditamento/" + accreditamentoId + "/validate"));
				return "redirect:/accreditamento/{accreditamentoId}/validate";
			}
		}catch(Exception ex){
			LOGGER.error(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/validate"),ex);
			model.addAttribute("accreditamentoId",sedeWrapper.getAccreditamentoId());
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			model.addAttribute("canValutaDomanda", accreditamentoService.canUserValutaDomanda(accreditamentoId, Utils.getAuthenticatedUser()));
			LOGGER.info(Utils.getLogMessage("VIEW: " + VALIDATE));
			return VALIDATE;
		}
	}

	/*** 	SAVE  ENABLEFIELD   ***/
	@RequestMapping(value = "/accreditamento/{accreditamentoId}/provider/{providerId}/sede/enableField", method = RequestMethod.POST)
	public String enableFieldSede(@ModelAttribute("sedeWrapper") SedeWrapper sedeWrapper,
									@ModelAttribute("richiestaIntegrazioneWrapper") RichiestaIntegrazioneWrapper richiestaIntegrazioneWrapper,
									@PathVariable Long accreditamentoId, @PathVariable Long providerId,
												Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "sede/enableField"));
		try{
			String errorMsg = enableFieldValidator.validate(richiestaIntegrazioneWrapper);
			if(errorMsg != null){
				model.addAttribute("sedeWrapper", prepareSedeWrapperEnableField(sedeService.getSede(sedeWrapper.getSede().getId()),sedeWrapper.getAccreditamentoId(),sedeWrapper.getProviderId()));
				model.addAttribute("message", new Message("message.errore", errorMsg, "error"));
				model.addAttribute("errorMsg", errorMsg);
				LOGGER.info(Utils.getLogMessage("VIEW: " + ENABLEFIELD));
				return ENABLEFIELD;
			}else{
				integrazioneService.saveEnableField(richiestaIntegrazioneWrapper);
				redirectAttrs.addFlashAttribute("message", new Message("message.completato", "message.campi_salvati", "success"));
			}
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "sede/enableField"),ex);
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("REDIRECT: /accreditamento/" + accreditamentoId + "/enableField"));
		}
		return "redirect:/accreditamento/{accreditamentoId}/enableField";
	};

	private String goToEdit(Model model, SedeWrapper sedeWrapper){
		model.addAttribute("sedeWrapper", sedeWrapper);
		LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
		return EDIT;
	}

	private String goToShow(Model model, SedeWrapper sedeWrapper){
		model.addAttribute("sedeWrapper", sedeWrapper);
		LOGGER.info(Utils.getLogMessage("VIEW: " + SHOW));
		return SHOW;
	}

	private String goToValidate(Model model, SedeWrapper sedeWrapper){
		model.addAttribute("sedeWrapper", sedeWrapper);
		LOGGER.info(Utils.getLogMessage("VIEW: " + VALIDATE));
		return VALIDATE;
	}

	private String goToEnableField(Model model, SedeWrapper sedeWrapper){
		model.addAttribute("sedeWrapper", sedeWrapper);
		model.addAttribute("richiestaIntegrazioneWrapper",integrazioneService.prepareRichiestaIntegrazioneWrapper(sedeWrapper.getAccreditamentoId(), SubSetFieldEnum.SEDE, sedeWrapper.getSede().getId()));
		LOGGER.info(Utils.getLogMessage("VIEW: " + ENABLEFIELD));
		return ENABLEFIELD;
	}

	private SedeWrapper prepareSedeWrapperEdit(Sede sede, long accreditamentoId, long providerId, AccreditamentoStatoEnum statoAccreditamento, boolean reloadByEditId) throws Exception{
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperEdit(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - entering"));

		SubSetFieldEnum subset = SubSetFieldEnum.SEDE;

		Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
		SedeWrapper sedeWrapper = new SedeWrapper();
		sedeWrapper.setSede(sede);
		sedeWrapper.setAccreditamentoId(accreditamentoId);
		sedeWrapper.setProviderId(providerId);
		sedeWrapper.setStatoAccreditamento(statoAccreditamento);
		sedeWrapper.setWrapperMode(AccreditamentoWrapperModeEnum.EDIT);
		sedeWrapper.setAccreditamento(accreditamento);

		if(sede.isNew()){
			sedeWrapper.setIdEditabili(IdFieldEnum.getAllForSubset(subset));
		}else{
			//la Segreteria se non è in uno stato di integrazione/preavviso rigetto può sempre modificare
			if (Utils.getAuthenticatedUser().getAccount().isSegreteria() && !(accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto() || accreditamento.isModificaDati()))
				sedeWrapper.setIdEditabili(IdFieldEnum.getAllForSubset(subset));
			else
				sedeWrapper.setIdEditabili(Utils.getSubsetOfIdFieldEnum(fieldEditabileAccreditamentoService.getAllFieldEditabileForAccreditamentoAndObject(accreditamentoId, sede.getId()), subset));
			//sedeWrapper.setFieldIntegrazione(Utils.getSubset(fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamentoAndObject(accreditamentoId, sede.getId()), subset));
		}

		if(accreditamento.isIntegrazione() || accreditamento.isPreavvisoRigetto() || accreditamento.isModificaDati()){
			Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
			AccreditamentoStatoEnum stato = accreditamento.getCurrentStato();
			prepareApplyIntegrazione(sedeWrapper, subset, reloadByEditId, accreditamentoId, stato, workFlowProcessInstanceId);
		}

		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperEdit(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}

	private void prepareValutazioneIntegrazioneReferee(SedeWrapper sedeWrapper, Long accreditamentoId,
			AccreditamentoStatoEnum stato, Long workFlowProcessInstanceId, SubSetFieldEnum subset) {
		prepareWrapperIntegrazioneFullSede(sedeWrapper, stato, accreditamentoId, workFlowProcessInstanceId, subset);
	}

	private void prepareApplyIntegrazione(SedeWrapper sedeWrapper, SubSetFieldEnum subset, boolean reloadByEditIt,
			Long accreditamentoId, AccreditamentoStatoEnum stato, Long workFlowProcessInstanceId) throws Exception{
		integrazioneService.detach(sedeWrapper.getSede());
		//nuova sede
		if(sedeWrapper.getSede() == null || sedeWrapper.getSede().getId() == null){
			sedeWrapper.getIdEditabili().addAll(IdFieldEnum.getAllForSubset(subset));
		}else{
			prepareWrapperIntegrazioneFullSede(sedeWrapper, stato, accreditamentoId, workFlowProcessInstanceId,subset);
			//modifica
			if(!reloadByEditIt)
				integrazioneService.applyIntegrazioneObject(sedeWrapper.getSede(), sedeWrapper.getFieldIntegrazione());
		}
	}

	private void prepareWrapperIntegrazioneFullSede(SedeWrapper sedeWrapper, AccreditamentoStatoEnum stato, Long accreditamentoId, Long workFlowProcessInstanceId, SubSetFieldEnum subset) {
		//prendo tutte le integrazioni fatte dal provider
		Set<FieldIntegrazioneAccreditamento> fieldIntegrazione = fieldIntegrazioneAccreditamentoService.getAllFieldIntegrazioneForAccreditamentoAndObjectByContainer(accreditamentoId, stato, workFlowProcessInstanceId, sedeWrapper.getSede().getId());
		//filtro per quelle del subset sede
		sedeWrapper.setFieldIntegrazione(Utils.getSubset(fieldIntegrazione, subset));

		//vedo se e' presente il filed FULL e setto la info nel wrapper
		sedeWrapper.setFullIntegrazione(Utils.getField(fieldIntegrazione, IdFieldEnum.SEDE__FULL));
	}

	private SedeWrapper prepareSedeWrapperShow(Sede sede, long accreditamentoId, long providerId){
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperShow(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - entering"));
		SedeWrapper sedeWrapper = new SedeWrapper();
		sedeWrapper.setSede(sede);
		sedeWrapper.setAccreditamentoId(accreditamentoId);
		sedeWrapper.setProviderId(providerId);
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperShow(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}

	private SedeWrapper prepareSedeWrapperValidate(Sede sede, long accreditamentoId, long providerId, AccreditamentoStatoEnum statoAccreditamento, boolean reloadByEditId) throws Exception{
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperValidate(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - entering"));

		SubSetFieldEnum subset = SubSetFieldEnum.SEDE;

		Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
		Long workFlowProcessInstanceId = accreditamento.getWorkflowInCorso().getProcessInstanceId();
		AccreditamentoStatoEnum stato = accreditamento.getCurrentStato();
		SedeWrapper sedeWrapper = new SedeWrapper();

		//carico la valutazione per l'utente
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountIdAndNotStoricizzato(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Map<IdFieldEnum, FieldValutazioneAccreditamento> mappa = new HashMap<IdFieldEnum, FieldValutazioneAccreditamento>();
		if(valutazione != null) {
			mappa = fieldValutazioneAccreditamentoService.filterFieldValutazioneByObjectAsMap(valutazione.getValutazioni(), sede.getId());
		}

		//cerco tutte le valutazioni del subset sede per ciascun valutatore dell'accreditamento
		Map<Account, Map<IdFieldEnum, FieldValutazioneAccreditamento>> mappaValutatoreValutazioni = new HashMap<Account, Map<IdFieldEnum, FieldValutazioneAccreditamento>>();
		mappaValutatoreValutazioni = valutazioneService.getMapValutatoreValutazioniByAccreditamentoIdAndObjectId(accreditamentoId, sede.getId());

		//prendo tutti gli id del subset
		Set<IdFieldEnum> idEditabili = new HashSet<IdFieldEnum>();
		idEditabili = IdFieldEnum.getAllForSubset(subset);


		sedeWrapper.setMappaValutatoreValutazioni(mappaValutatoreValutazioni);
		sedeWrapper.setIdEditabili(idEditabili);
		sedeWrapper.setMappa(mappa);
		sedeWrapper.setSede(sede);
		sedeWrapper.setAccreditamentoId(accreditamentoId);
		sedeWrapper.setProviderId(providerId);
		sedeWrapper.setStatoAccreditamento(statoAccreditamento);
		sedeWrapper.setWrapperMode(AccreditamentoWrapperModeEnum.VALIDATE);

		if(accreditamento.isValutazioneSegreteria() || accreditamento.isValutazioneSegreteriaVariazioneDati()){
			stato = accreditamento.getStatoUltimaIntegrazione();
			prepareApplyIntegrazione(sedeWrapper, subset, reloadByEditId, accreditamentoId, stato, workFlowProcessInstanceId);
		}

		//solo se la valutazione è del crecm / team leader dopo l'INTEGRAZIONE
		if(accreditamento.getStatoUltimaIntegrazione() != null && (accreditamento.isValutazioneCrecm() || accreditamento.isValutazioneTeamLeader() || accreditamento.isValutazioneCrecmVariazioneDati())){
			stato = accreditamento.getStatoUltimaIntegrazione();
			prepareValutazioneIntegrazioneReferee(sedeWrapper, accreditamentoId, stato, workFlowProcessInstanceId, subset);
		}

		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperValidate(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}

	private SedeWrapper prepareSedeWrapperEnableField(Sede sede, long accreditamentoId, long providerId){
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperEnableField(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - entering"));
		SedeWrapper sedeWrapper = prepareSedeWrapperShow(sede, accreditamentoId, providerId);
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperEnableField(" + sede.getId() + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}

	/***	LOGICA PER SALVATAGGIO SEDE	***/
	private void saveSede(SedeWrapper sedeWrapper, Provider provider) throws Exception{
		LOGGER.info(Utils.getLogMessage("Salvataggio sede"));

		boolean insertFieldEditabile = (sedeWrapper.getSede().isNew()) ? true : false;
		//non inserisce i field editabili se è la segreteria a fare l'inserimento in uno stato dell'accreditamento che non sia Bozza
		insertFieldEditabile = (Utils.getAuthenticatedUser().getAccount().isSegreteria() && !accreditamentoService.getAccreditamento(sedeWrapper.getAccreditamentoId()).isBozza()) ? false : true;

		sedeService.save(sedeWrapper.getSede(), provider);

		//inserimento nuova sede in Domanda di Accreditamento
		//inseriamo gli IdEditabili (con riferimento all'id nel caso di multi-istanza) per consentire le modifiche successive
		if(insertFieldEditabile){
			fieldEditabileAccreditamentoService.insertFieldEditabileForAccreditamento(sedeWrapper.getAccreditamentoId(), sedeWrapper.getSede().getId(), SubSetFieldEnum.SEDE, IdFieldEnum.getAllForSubset(SubSetFieldEnum.SEDE));
		}
	}

	private void integra(SedeWrapper wrapper, boolean eliminazione) throws Exception{
		LOGGER.info(Utils.getLogMessage("Integrazione sede"));
		Accreditamento accreditamento = wrapper.getAccreditamento() != null ? wrapper.getAccreditamento() : accreditamentoService.getAccreditamento(wrapper.getAccreditamentoId());
		WorkflowInfo workflowInCorso = accreditamento.getWorkflowInCorso();

		List<FieldIntegrazioneAccreditamento> fieldIntegrazioneList = new ArrayList<FieldIntegrazioneAccreditamento>();
		IdFieldEnum idFieldFull = IdFieldEnum.SEDE__FULL;

		if(!eliminazione){
			//Creazione Sede multistanza
			if(wrapper.getSede().isNew()){
				//registriamo inserimento nuova sede come dirty object
				wrapper.getSede().setDirty(true);
				sedeService.save(wrapper.getSede(), providerService.getProvider(wrapper.getProviderId()));
				String json = jacksonObjectMapper.writerWithView(JsonViewModel.Integrazione.class).writeValueAsString(wrapper.getSede());
				LOGGER.info(Utils.getLogMessage("Salvataggio fieldIntegrazione per creazione sede: " + json));
				fieldIntegrazioneList.add(new FieldIntegrazioneAccreditamento(idFieldFull, accreditamento, wrapper.getSede().getId(), json, TipoIntegrazioneEnum.CREAZIONE));
			}else{
				//MODIFICA SINGOLO CAMPO
				for(IdFieldEnum idField : wrapper.getIdEditabili()){
					fieldIntegrazioneList.add(new FieldIntegrazioneAccreditamento (idField, accreditamento, wrapper.getSede().getId(), integrazioneService.getField(wrapper.getSede(), idField.getNameRef()), TipoIntegrazioneEnum.MODIFICA));
				}
			}
		}else{
			LOGGER.info(Utils.getLogMessage("Salvataggio fieldIntegrazione per eliminazione sede: " + wrapper.getSede().getId()));
			fieldIntegrazioneList.add(new FieldIntegrazioneAccreditamento(idFieldFull, accreditamento, wrapper.getSede().getId(), wrapper.getSede().getId(), TipoIntegrazioneEnum.ELIMINAZIONE));
		}

		AccreditamentoStatoEnum stato = null;
		if(accreditamento.isVariazioneDati())
			stato = accreditamento.getStatoVariazioneDati();
		else stato = accreditamento.getStato();
		fieldIntegrazioneAccreditamentoService.update(wrapper.getFieldIntegrazione(), fieldIntegrazioneList, accreditamento.getId(), workflowInCorso.getProcessInstanceId(), stato);
	}
}
