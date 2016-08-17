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

import it.tredi.ecm.dao.entity.Account;
import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.FieldValutazioneAccreditamento;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.Sede;
import it.tredi.ecm.dao.entity.Valutazione;
import it.tredi.ecm.dao.enumlist.Costanti;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import it.tredi.ecm.dao.enumlist.SubSetFieldEnum;
import it.tredi.ecm.service.FieldEditabileAccreditamentoService;
import it.tredi.ecm.service.FieldValutazioneAccreditamentoService;
import it.tredi.ecm.service.ProviderService;
import it.tredi.ecm.service.SedeService;
import it.tredi.ecm.service.ValutazioneService;
import it.tredi.ecm.utils.Utils;
import it.tredi.ecm.web.bean.Message;
import it.tredi.ecm.web.bean.SedeWrapper;
import it.tredi.ecm.web.validator.SedeValidator;
import it.tredi.ecm.web.validator.ValutazioneValidator;

@Controller
public class SedeController {
	public static final Logger LOGGER = LoggerFactory.getLogger(SedeController.class);

	private final String EDIT = "sede/sedeEdit";
	private final String SHOW = "sede/sedeShow";
	private final String VALIDATE = "sede/sedeValidate";

	@Autowired private SedeService sedeService;
	@Autowired private ProviderService providerService;
	@Autowired private FieldEditabileAccreditamentoService fieldEditabileService;
	@Autowired private SedeValidator sedeValidator;
	@Autowired private FieldValutazioneAccreditamentoService fieldValutazioneAccreditamentoService;
	@Autowired private ValutazioneValidator valutazioneValidator;
	@Autowired private ValutazioneService valutazioneService;

	/***	GLOBAL MODEL ATTRIBUTES	***/
	@ModelAttribute("elencoProvince")
	public List<String> getElencoProvince(){
		List<String> elencoProvince = new ArrayList<String>();

		elencoProvince.add("Venezia");
		elencoProvince.add("Padova");
		elencoProvince.add("Verona");

		return elencoProvince;
	}

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
	public SedeWrapper getSede(@RequestParam(name = "editId", required = false) Long id){
		if(id != null){
			SedeWrapper sedeWrapper = new SedeWrapper();
			sedeWrapper.setSede(sedeService.getSede(id));
			return sedeWrapper;
		}
		return new SedeWrapper();
	}

	@PreAuthorize("@securityAccessServiceImpl.canEditAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canEditProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/copia")
	public String getCopiaSedeLegale(@PathVariable Long accreditamentoId, @PathVariable Long providerId, Model model){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/copia"));
		try {
			Sede sedeLegale = new Sede();
			Provider provider = providerService.getProvider(providerId);
			if(provider != null){
				sedeLegale = provider.getSedeLegale();
			}

			if(sedeLegale != null){
				//preparo il wrapper specificando che è una sedeOperativa
				//magicnumber (rende non editabili tutti i campi della sede operativa, ma consente ugualmente il salvataggio)
				//refresh solo del fragment della view
				SedeWrapper sedeWrapper = prepareSedeWrapperEdit(sedeLegale, Costanti.SEDE_OPERATIVA, accreditamentoId, providerId);
				sedeWrapper.setCoincide(true);
				//setIdEditabili(Arrays.asList(0));
				return goToEditWhitFragment(model, sedeWrapper, "content");
			}else{
				model.addAttribute("message", new Message("message.warning", "message.manca_sedeLegale", "warning"));
				LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
				return EDIT + ":: content";
			}
		}catch (Exception ex) {
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/copia"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
			return EDIT;
		}
	}

	/***	NEW / EDIT 	***/
	@PreAuthorize("@securityAccessServiceImpl.canEditAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canEditProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/new")
	public String getNewSedeCurrentProvider(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @RequestParam(value = "tipologiaSede", required = true) String tipologiaSede,
			Model model, RedirectAttributes redirectAttrs) throws Exception{
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/new"));
		try {
			return goToEdit(model, prepareSedeWrapperEdit(new Sede(), tipologiaSede, accreditamentoId, providerId));
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/new"),ex);
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId));
			return "redirect:/accreditamento" + accreditamentoId;
		}
	}

	/*** EDIT SEDE ***/
	@PreAuthorize("@securityAccessServiceImpl.canEditAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canEditProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/edit")
	public String editSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id,
			@RequestParam(value = "tipologiaSede", required = true) String tipologiaSede, Model model){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/edit"));
		try {
			SedeWrapper sedeWrapper = prepareSedeWrapperEdit(sedeService.getSede(id), tipologiaSede, accreditamentoId, providerId);

			if(tipologiaSede.equals(Costanti.SEDE_OPERATIVA)){
				Provider provider = providerService.getProvider(providerId);
				if(provider != null){
					if(provider.getSedeLegale() != null && provider.getSedeOperativa() != null){
						if(provider.getSedeLegale().getId().equals(provider.getSedeOperativa().getId())){
							sedeWrapper.setCoincide(true);
						}
					}
				}
			}
			return goToEdit(model, sedeWrapper);
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/edit"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
			return EDIT;
		}
	}

	/*** VALUTAZIONE SEDE ***/
//	@PreAuthorize("@securityAccessServiceImpl.canValidateAccreditamento(principal,#accreditamentoId) TODO
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/validate")
	public String validateSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id,
			@RequestParam (value = "tipologiaSede", required = true) String tipologiaSede, Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/validate"));
		try {
			SedeWrapper sedeWrapper = prepareSedeWrapperValidate(sedeService.getSede(id), tipologiaSede, accreditamentoId, providerId);
			return goToValidate(model, sedeWrapper);
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/validate"),ex);
			model.addAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("VIEW: " + VALIDATE));
			return VALIDATE;
		}
	}

	/*** SHOW SEDE ***/
	@PreAuthorize("@securityAccessServiceImpl.canShowAccreditamento(principal,#accreditamentoId) and @securityAccessServiceImpl.canShowProvider(principal,#providerId)")
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{providerId}/sede/{id}/show")
	public String showSede(@PathVariable Long accreditamentoId, @PathVariable Long providerId, @PathVariable Long id,
			@RequestParam (value = "tipologiaSede", required = true) String tipologiaSede, @RequestParam(value = "from", required =  false) String from, Model model, RedirectAttributes redirectAttrs){
		LOGGER.info(Utils.getLogMessage("GET /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/show"));
		try {
			if (from != null) {
				redirectAttrs.addFlashAttribute("tipologiaSede", tipologiaSede);
				redirectAttrs.addFlashAttribute("mode", from);
				return "redirect:/accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/" + id + "/show?tipologiaSede=" + tipologiaSede;
			}
			SedeWrapper sedeWrapper = prepareSedeWrapperShow(sedeService.getSede(id), tipologiaSede, accreditamentoId, providerId);
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
			sedeValidator.validate(sedeWrapper.getSede(), result, "sede.");
			if(result.hasErrors()){
				model.addAttribute("message",new Message("message.errore", "message.inserire_campi_required", "error"));
				LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT));
				return EDIT;
			}else{
				sedeService.save(sedeWrapper.getSede(), providerService.getProvider(providerId), sedeWrapper.getTipologiaSede());
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

	/***	SALVA VALUTAZIONE 	***/
	@RequestMapping(value = "/accreditamento/{accreditamentoId}/provider/{providerId}/sede/validate", method = RequestMethod.POST)
	public String valutaSede(@ModelAttribute("sedeWrapper") SedeWrapper sedeWrapper, BindingResult result,
			Model model, @PathVariable Long providerId, RedirectAttributes redirectAttrs, @PathVariable Long accreditamentoId){
		LOGGER.info(Utils.getLogMessage("POST /accreditamento/" + accreditamentoId + "/provider/" + providerId + "/sede/validate"));
		try{
			//validazione della sede
			valutazioneValidator.validateValutazione(sedeWrapper.getMappa(), result);
			if(result.hasErrors()){
				model.addAttribute("message",new Message("message.errore", "message.inserire_campi_required", "error"));
				LOGGER.info(Utils.getLogMessage("VIEW: " + VALIDATE));
				return VALIDATE;
			}else{
				Accreditamento accreditamento = new Accreditamento();
				accreditamento.setId(sedeWrapper.getAccreditamentoId());
				sedeWrapper.getMappa().forEach((k, v) -> {
					v.setIdField(k);
					v.setAccreditamento(accreditamento);
				});
				Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountId(accreditamento.getId(), Utils.getAuthenticatedUser().getAccount().getId());
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
			LOGGER.info(Utils.getLogMessage("VIEW: " + VALIDATE));
			return VALIDATE;
		}
	}

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

	private String goToEditWhitFragment(Model model, SedeWrapper sedeWrapper, String fragment){
		model.addAttribute("sedeWrapper", sedeWrapper);
		LOGGER.info(Utils.getLogMessage("VIEW: " + EDIT + " :: " + fragment));
		return EDIT + " :: " + fragment;
	}

	private SedeWrapper prepareSedeWrapperEdit(Sede sede, String tipologiaSede, long accreditamentoId, long providerId){
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperEdit(" + sede.getId() + "," + tipologiaSede + "," + accreditamentoId + "," + providerId +") - entering"));
		SedeWrapper sedeWrapper = new SedeWrapper();

		sedeWrapper.setSede(sede);
		sedeWrapper.setTipologiaSede(tipologiaSede);

		SubSetFieldEnum subset = (tipologiaSede.equals(Costanti.SEDE_LEGALE)) ? SubSetFieldEnum.SEDE_LEGALE : SubSetFieldEnum.SEDE_OPERATIVA;

		if(sede.isNew())
			sedeWrapper.setIdEditabili(IdFieldEnum.getAllForSubset(subset));
		else
			sedeWrapper.setIdEditabili(Utils.getSubsetOfIdFieldEnum(fieldEditabileService.getAllFieldEditabileForAccreditamento(accreditamentoId), subset));

		sedeWrapper.setAccreditamentoId(accreditamentoId);
		sedeWrapper.setProviderId(providerId);
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperEdit(" + sede.getId() + "," + tipologiaSede + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}

	private SedeWrapper prepareSedeWrapperShow(Sede sede, String tipologiaSede, long accreditamentoId, long providerId){
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperShow(" + sede.getId() + "," + tipologiaSede + "," + accreditamentoId + "," + providerId +") - entering"));
		SedeWrapper sedeWrapper = new SedeWrapper();
		sedeWrapper.setSede(sede);
		sedeWrapper.setTipologiaSede(tipologiaSede);
		sedeWrapper.setAccreditamentoId(accreditamentoId);
		sedeWrapper.setProviderId(providerId);
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperShow(" + sede.getId() + "," + tipologiaSede + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}

	private SedeWrapper prepareSedeWrapperValidate(Sede sede, String tipologiaSede, long accreditamentoId, long providerId){
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperValidate(" + sede.getId() + "," + tipologiaSede + "," + accreditamentoId + "," + providerId +") - entering"));
		SedeWrapper sedeWrapper = new SedeWrapper();

		//carico la valutazione per l'utente
		Valutazione valutazione = valutazioneService.getValutazioneByAccreditamentoIdAndAccountId(accreditamentoId, Utils.getAuthenticatedUser().getAccount().getId());
		Map<IdFieldEnum, FieldValutazioneAccreditamento> mappa = new HashMap<IdFieldEnum, FieldValutazioneAccreditamento>();

		//cerco tutte le valutazioni del subset sede per ciascun valutatore dell'accreditamento
		Map<Account, Map<IdFieldEnum, FieldValutazioneAccreditamento>> mappaValutatoreValutazioni = new HashMap<Account, Map<IdFieldEnum, FieldValutazioneAccreditamento>>();

		//prendo tutti gli id del subset
		Set<IdFieldEnum> idEditabili = new HashSet<IdFieldEnum>();

		if(valutazione != null) {
			if(tipologiaSede.equals("SedeLegale")) {
				mappa = fieldValutazioneAccreditamentoService.filterFieldValutazioneBySubSetAsMap(valutazione.getValutazioni(), SubSetFieldEnum.SEDE_LEGALE);
				mappaValutatoreValutazioni = valutazioneService.getMapValutatoreValutazioniByAccreditamentoIdAndSubSet(accreditamentoId, SubSetFieldEnum.SEDE_LEGALE);
				idEditabili = IdFieldEnum.getAllForSubset(SubSetFieldEnum.SEDE_LEGALE);
			}
			if(tipologiaSede.equals("SedeOperativa")) {
				mappa = fieldValutazioneAccreditamentoService.filterFieldValutazioneBySubSetAsMap(valutazione.getValutazioni(), SubSetFieldEnum.SEDE_OPERATIVA);
				mappaValutatoreValutazioni  = valutazioneService.getMapValutatoreValutazioniByAccreditamentoIdAndSubSet(accreditamentoId, SubSetFieldEnum.SEDE_OPERATIVA);
				idEditabili = IdFieldEnum.getAllForSubset(SubSetFieldEnum.SEDE_OPERATIVA);
			}
		}

		sedeWrapper.setMappaValutatoreValutazioni(mappaValutatoreValutazioni);
		sedeWrapper.setIdEditabili(idEditabili);
		sedeWrapper.setMappa(mappa);
		sedeWrapper.setSede(sede);
		sedeWrapper.setTipologiaSede(tipologiaSede);
		sedeWrapper.setAccreditamentoId(accreditamentoId);
		sedeWrapper.setProviderId(providerId);
		LOGGER.info(Utils.getLogMessage("prepareSedeWrapperValidate(" + sede.getId() + "," + tipologiaSede + "," + accreditamentoId + "," + providerId +") - exiting"));
		return sedeWrapper;
	}
}
