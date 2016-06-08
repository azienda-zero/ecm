package it.tredi.ecm.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.enumlist.TipoOrganizzatore;
import it.tredi.ecm.service.ProviderService;
import it.tredi.ecm.service.bean.CurrentUser;
import it.tredi.ecm.service.bean.ProviderRegistrationWrapper;
import it.tredi.ecm.utils.Utils;
import it.tredi.ecm.web.bean.Message;
import it.tredi.ecm.web.validator.ProviderRegistrationWrapperValidator;

@Controller
public class ProviderRegistrationController {
	
	@Autowired
	private ProviderService providerService;
	
	@Autowired
	private ProviderRegistrationWrapperValidator providerRegistrationValidator;
	
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}
	
	@ModelAttribute("tipoOrganizzatoreList")
	public TipoOrganizzatore[] getListTipoOrganizzatore(){
		return TipoOrganizzatore.values();
	}
	
	@ModelAttribute("providerForm")
	public ProviderRegistrationWrapper getProviderRegistrationWrapperPreRequest(@RequestParam(value="editId",required = false) Long id){
		return providerService.getProviderRegistrationWrapper();
	}

	/** Public provider registration form. */
	@RequestMapping(value = "/providerRegistration", method = RequestMethod.GET)
	public String providerRegistration(Model model, RedirectAttributes redirectAttrs) {
		try {
			model.addAttribute("providerForm", providerService.getProviderRegistrationWrapper());
			model.addAttribute("stepToShow", 0);
			return "providerRegistration";
		}catch (Exception ex){
			//TODO gestione eccezione
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			return "redirect:/home";
		}
	}
	
	/** Private provider registration form for authenticated users. */
	@RequestMapping("/providerRegistration/edit")
	public String editProviderRegistration(Model model, RedirectAttributes redirectAttrs){
		try {
			model.addAttribute("providerForm", providerService.getProviderRegistrationWrapper());
			return "provider/editProvider";
		}catch (Exception ex){
			//TODO gestione eccezione
			redirectAttrs.addFlashAttribute("message", new Message("message.errore", "message.errore_eccezione", "error"));
			return "redirect:/home";
		}
	}

	@RequestMapping(value = "/providerRegistration", method = RequestMethod.POST)
	public String registraProvider(@ModelAttribute("providerForm") ProviderRegistrationWrapper providerRegistrationWrapper, 
									BindingResult result, RedirectAttributes redirectAttrs, Model model, 
									@RequestParam( value="saveTypeMinimal",required = false) String saveTypeMinimal,
									@RequestParam( value="saveTypeFull",required = false) String saveTypeFull,
									@RequestParam( value="delegaRichiedente",required = false) MultipartFile multiPartFile){
		try{
			//validazione solo del provider oppure dell'intero form?
			boolean saveMinimal = (saveTypeMinimal != null && saveTypeFull == null) ? true : false; 
			
			if(!saveMinimal){
				File delegaRichiedenteFile = Utils.convertFromMultiPart(multiPartFile);
				if(delegaRichiedenteFile != null)
					providerRegistrationWrapper.setDelegaRichiedenteFile(delegaRichiedenteFile);
			}
			
			providerRegistrationValidator.validate(providerRegistrationWrapper, result, saveMinimal);	
			
			if(result.hasErrors()){
				model.addAttribute("stepToShow", evaluateErrorStep(result));
				model.addAttribute("message",new Message("message.errore", "message.inserire_campi_required", "error"));
				return returnToProviderRegistrationForm();
			}else{
				providerService.saveProviderRegistrationWrapper(providerRegistrationWrapper, saveMinimal);
				redirectAttrs.addFlashAttribute("message", new Message("message.completato", "message.provider_salvato", "success"));
				return "redirect:/home";
			}
		}catch (Exception ex){
			model.addAttribute("stepToShow", evaluateErrorStep(result));
			return returnToProviderRegistrationForm();
		}
		
	}
	
	private int evaluateErrorStep(BindingResult result){
		if(result.hasFieldErrors("provider*"))
			return 0;
		if(result.hasFieldErrors("richiedente*") || result.hasFieldErrors("delegaRichiedente*"))
			return 1;
		if(result.hasFieldErrors("legale*"))
			return 2;
		return 0;
	}
			
	private String returnToProviderRegistrationForm(){
		CurrentUser currentUser = Utils.getAuthenticatedUser();
		if(currentUser != null){
			return "provider/editProvider";
		}else{
			return "providerRegistration";
		}
	}
}
