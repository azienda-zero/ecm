package it.tredi.ecm.web;

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

import it.tredi.ecm.dao.entity.Profile;
import it.tredi.ecm.service.ProfileAndRoleService;
import it.tredi.ecm.web.validator.ProfileValidator;

@Controller
public class ProfileController {
	@Autowired
	private ProfileAndRoleService profileAndRoleService;
	@Autowired
	private ProfileValidator profileValidator;
	
	@InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

	@RequestMapping("profile/list")
	public String showAll(Model model){
		model.addAttribute("profileList", profileAndRoleService.getAllProfile());
		return "user/profileList";
	}
	
	@RequestMapping("profile/{id}/edit")
	public String editProfile(@PathVariable Long id, Model model){
		model.addAttribute("profile", profileAndRoleService.getProfile(id));
		model.addAttribute("roleList", profileAndRoleService.getAllRole());
		return "user/editProfile";
	}
	
	@RequestMapping("profile/new")
	public String newProfile(Model model){
		model.addAttribute("profile", new Profile());
		model.addAttribute("roleList", profileAndRoleService.getAllRole());
		return "user/editProfile";
	}
	
	@RequestMapping(value = "profile/save", method = RequestMethod.POST)
	public String saveProfile(@ModelAttribute("profile") Profile profile, BindingResult result, Model model){
		profileValidator.validate(profile, result);
		if(result.hasErrors()){
			model.addAttribute("roleList", profileAndRoleService.getAllRole());
			return "user/editProfile";
		}else{
			profileAndRoleService.saveProfile(profile);
			return "redirect:/profile/list";
		}
	}
	
	@ModelAttribute("profile")
	public Profile getProfilePreRequest(@RequestParam(value="editId",required = false) Long id){
		if(id != null)
			return profileAndRoleService.getProfile(id);
		return new Profile();
	}
}
