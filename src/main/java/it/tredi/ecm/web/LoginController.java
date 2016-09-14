package it.tredi.ecm.web;

import java.util.Iterator;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.tredi.ecm.dao.entity.Profile;
import it.tredi.ecm.dao.entity.Seduta;
import it.tredi.ecm.dao.enumlist.AccreditamentoStatoEnum;
import it.tredi.ecm.dao.enumlist.AccreditamentoTipoEnum;
import it.tredi.ecm.service.AccountService;
import it.tredi.ecm.service.AccreditamentoService;
import it.tredi.ecm.service.ProviderService;
import it.tredi.ecm.service.SedutaService;
import it.tredi.ecm.service.bean.CurrentUser;
import it.tredi.ecm.utils.Utils;
import it.tredi.ecm.web.bean.HomeWrapper;
import it.tredi.ecm.web.bean.Message;

@Controller
public class LoginController {
	public static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

	@Autowired private AccreditamentoService accreditamentoService;
	@Autowired private ProviderService providerService;
	@Autowired private AccountService accountService;
	@Autowired private SedutaService sedutaService;

	@RequestMapping("/")
	public String root(Locale locale) {
		return "redirect:/home";
	}

	/** Home page. */
	@RequestMapping("/home")
	public String home(Model model, RedirectAttributes redirectAttrs) {
		LOGGER.info(Utils.getLogMessage("GET /home"));

		try{
			//Check del profilo del utente loggato
			CurrentUser currentUser = Utils.getAuthenticatedUser();
			return goToShow(model, prepareHomeWrapper(currentUser), redirectAttrs);
		}catch (Exception ex) {
			LOGGER.error(Utils.getLogMessage("GET /home"),ex);
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("REDIRECT: /login"));
			return "redirect:/login";
		}
	}

	private HomeWrapper prepareHomeWrapper(CurrentUser currentUser) {
		HomeWrapper wrapper = new HomeWrapper();
		Iterator<Profile> iterator = currentUser.getAccount().getProfiles().iterator();
		while(iterator.hasNext()) {
			switch(iterator.next().getProfileEnum()) {
				case ADMIN:
					//TODO riempe i dati relativi ad admin
					wrapper.setIsAdmin(true);
					wrapper.setUtentiInAttesaDiAttivazione(1);
					break;
				case PROVIDER:
					//TODO riempe i dati relativi al provider
					wrapper.setIsProvider(true);
					wrapper.setProviderId(providerService.getProviderIdByAccountId(currentUser.getAccount().getId()));
					wrapper.setEventiDaPagare(3);
					wrapper.setMessaggi(9);
					wrapper.setAccreditamentiDaIntegrare(accreditamentoService.countAllAccreditamentiByStatoAndProviderId(AccreditamentoStatoEnum.INTEGRAZIONE, wrapper.getProviderId()));
					break;
				case SEGRETERIA:
					//TODO riempe i dati relativi alla segreteria
					wrapper.setIsSegreteria(true);
					wrapper.setDomandeProvvisorieNotTaken(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA_ASSEGNAMENTO, AccreditamentoTipoEnum.PROVVISORIO, true));
					wrapper.setDomandeStandardNotTaken(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA_ASSEGNAMENTO, AccreditamentoTipoEnum.STANDARD, true));
					wrapper.setDomandeAssegnamento(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum.ASSEGNAMENTO, null, null));
					wrapper.setDomandeProvvisorieRichiestaIntegrazione(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum.RICHIESTA_INTEGRAZIONE, AccreditamentoTipoEnum.PROVVISORIO, null));
					wrapper.setDomandeProvvisorieValutazioneIntegrazione(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum.VALUTAZIONE_SEGRETERIA, AccreditamentoTipoEnum.PROVVISORIO, null));
					wrapper.setDomandeProvvisoriePreavvisoRigetto(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomanda(AccreditamentoStatoEnum.PREAVVISO_RIGETTO, AccreditamentoTipoEnum.PROVVISORIO, null));
					wrapper.setDomandeInScadenza(accreditamentoService.countAllAccreditamentiInScadenza());
					wrapper.setBadReferee(accountService.countAllRefereeWithValutazioniNonDate());
					break;
				case REFEREE:
					//TODO riempe i dati relativi al referee
					wrapper.setIsReferee(true);
					wrapper.setDomandeInCarica(accreditamentoService.countAllAccreditamentiByStatoAndTipoDomandaForAccountId(AccreditamentoStatoEnum.VALUTAZIONE_CRECM, null, Utils.getAuthenticatedUser().getAccount().getId()));
					wrapper.setDomandeNonValutateConsecutivamente(accountService.getUserById(currentUser.getAccount().getId()).getValutazioniNonDate());
				case COMMISSIONE:
					//TODO riempe i dati relativi alla commissione
					wrapper.setIsCommissione(true);
					wrapper.setProssimaSeduta(sedutaService.getNextSeduta());
			}
		}
		return wrapper;
	}

	private String goToShow(Model model, HomeWrapper wrapper, RedirectAttributes redirectAttrs) {
		try {
			model.addAttribute("homeWrapper", wrapper);
			LOGGER.info(Utils.getLogMessage("VIEW: /home"));
			return "home";
		}catch (Exception ex){
			LOGGER.error(Utils.getLogMessage("goToShow"),ex);
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			LOGGER.info(Utils.getLogMessage("REDIRECT: /login"));
			return "redirect:/login";
		}
	}

	/** Login form. */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login() {
		return "login";
	}

	/** Main form. */
	@RequestMapping(value = "/main", method = RequestMethod.GET)
	public String main() {
		return "main";
	}
}
