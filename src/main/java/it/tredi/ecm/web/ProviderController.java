package it.tredi.ecm.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.enumlist.TipoOrganizzatore;
import it.tredi.ecm.service.ProviderService;
import it.tredi.ecm.web.validator.ProviderValidator;

@Controller
public class ProviderController {
	
	@Autowired
	private ProviderService providerService;
	@Autowired
	private ProviderValidator providerValidator;
	
	@InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

	/*** GLOBAL MODEL ATTRIBUTES***/
	@ModelAttribute("elencoRagioniSociali")
	public List<String> getListRagioniSociali(){
		//TODO recuperare elenco ragioni sociali
		List<String> ragioniSociali = new ArrayList<String>();
		ragioniSociali.add("srl");
		ragioniSociali.add("snc");
		ragioniSociali.add("spa");
		ragioniSociali.add("sas");
		return ragioniSociali;
	}
	
	@ModelAttribute("tipoOrganizzatoreList")
	public TipoOrganizzatore[] getListTipoOrganizzatore(){
		return TipoOrganizzatore.values();
	}
	
	@ModelAttribute("provider")
	public Provider getProvider(@RequestParam(name = "editId", required = false) Long id){
		if(id != null)
			return providerService.getProvider(id);
		return new Provider();
	}
	/*** GLOBAL MODEL ATTRIBUTES***/
	
	/***	SHOW	***/
	@RequestMapping("provider/show")
	public String showProviderFromCurrentUser(Model model){
		return goToShowProvider(model, providerService.getProvider());
	}
	
	@RequestMapping("provider/{id}/show")
	public String showProvider(@PathVariable Long id, Model model){
		return goToShowProvider(model, providerService.getProvider(id));
	}
	
	private String goToShowProvider(Model model, Provider provider){
		model.addAttribute("provider",provider);
		return "provider/showProvider";
	}
	
	/***	EDIT	***/
	@RequestMapping("provider/{id}/edit")
	public String editProvider(@PathVariable Long id, Model model){
		model.addAttribute("provider",providerService.getProvider(id));
		return "provider/providerEdit";
	}
	
	@RequestMapping("/accreditamento/{accreditamentoId}/provider/{id}/edit")
	public String editProviderFromAccreditamento(@PathVariable Long accreditamentoId, @PathVariable Long id, Model model){
		model.addAttribute("accreditamentoId",accreditamentoId);
		model.addAttribute("provider",providerService.getProvider(id));
		return "provider/providerEdit";
	}
	
	/***	SAVE	***/
	@RequestMapping(value = "/accreditamento/{accreditamentoId}/provider/save", method = RequestMethod.POST)
	public String salvaProvider(@PathVariable Long accreditamentoId, @ModelAttribute("provider") Provider provider, 
									BindingResult result, Model model, RedirectAttributes redirectAttrs){
		try{
			//validazione del provider
			providerValidator.validateForAccreditamento(provider, result, "");	
			
			if(result.hasErrors()){
				model.addAttribute("accreditamentoId",accreditamentoId);
				return "provider/providerEdit";
			}else{
				providerService.save(provider);
				redirectAttrs.addAttribute("accreditamentoId", accreditamentoId);
				return "redirect:/accreditamento/{accreditamentoId}";
			}
		}catch (Exception ex){
			model.addAttribute("accreditamentoId",accreditamentoId);
			return "provider/providerEdit";
		}
	}
	
	//TODO completare showallprovider
	@RequestMapping("provider/list")
	public String showAll(Model model){
		model.addAttribute("providerList", providerService.getAll());
		return "provider/showList";
	}
	
}
