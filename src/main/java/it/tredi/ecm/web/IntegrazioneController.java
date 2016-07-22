package it.tredi.ecm.web;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.FieldEditabile;
import it.tredi.ecm.dao.enumlist.IdFieldEnum;
import it.tredi.ecm.dao.enumlist.SubSetFieldEnum;
import it.tredi.ecm.dao.repository.FieldEditabileRepository;
import it.tredi.ecm.service.AccreditamentoService;
import it.tredi.ecm.utils.Utils;
import it.tredi.ecm.web.bean.TestWrapper;

@Controller
@SessionAttributes("wrapper")
public class IntegrazioneController {

	@Autowired AccreditamentoService accreditamentoService;
	@Autowired FieldEditabileRepository repo;

	public TestWrapper getWrapper(@RequestParam(value="editId",required = false) Long id){
		if(id != null)
			return prepareWrapper(accreditamentoService.getAccreditamento(id));
		return null;
	}

	@RequestMapping("/edit/{accreditamentoId}")
	public String edit(@PathVariable Long accreditamentoId, Model model){
		Accreditamento accreditamento = accreditamentoService.getAccreditamento(accreditamentoId);
		model.addAttribute("wrapper", prepareWrapper(accreditamento));
		return "test";
	}

	@RequestMapping("/edit/save")
	public String save(@ModelAttribute("wrapper") TestWrapper wrapper, Model model){

		Set<IdFieldEnum> listaDaView = wrapper.getSelected();

		Set<FieldEditabile> listaFull = wrapper.getFullLista();
		Set<FieldEditabile> listaSubset = Utils.getSubset(listaFull, SubSetFieldEnum.RESPONSABILE_SEGRETERIA);

		Accreditamento accreditamento = accreditamentoService.getAccreditamento(wrapper.getAccreditamento().getId());

		listaSubset.forEach(f -> {
			if(listaDaView == null || !listaDaView.contains(f.getIdField())){
				repo.delete(f);
			}
		});

		if(listaDaView != null){
			for(IdFieldEnum id : listaDaView){
				if(Utils.getField(listaFull,id) == null){
					FieldEditabile field = new FieldEditabile();
					field.setAccreditamento(accreditamento);
					field.setIdField(id);
					repo.save(field);
				}
			}
		}

		return "redirect:/home";
	}

	private TestWrapper prepareWrapper(Accreditamento accreditamento){
		TestWrapper wrapper = new TestWrapper();
		Set<FieldEditabile> fullLista = repo.findAllByAccreditamentoId(accreditamento.getId());

		wrapper.setAccreditamento(accreditamento);
		wrapper.setFullLista(fullLista);
		wrapper.setSelected(Utils.getSubsetOfIdFieldEnum(fullLista, SubSetFieldEnum.RESPONSABILE_SEGRETERIA));

		return wrapper;
	}
}
