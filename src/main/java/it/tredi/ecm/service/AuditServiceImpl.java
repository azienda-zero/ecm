package it.tredi.ecm.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.javers.core.Javers;
import org.javers.repository.jql.InstanceIdDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import it.tredi.ecm.audit.AuditLabelInfo;
import it.tredi.ecm.service.bean.CurrentUser;
import it.tredi.ecm.utils.Utils;

@Service
public class AuditServiceImpl implements AuditService {
	private static Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

	@Autowired
	private Javers javers;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private WorkflowService workflowService;

	@Value("#{PropertyMapper.startWith('auditlabelmap', '', false)}")
	private Map<String, String> labelForAuditLabel;

	@Override
	public 	String getLabelForAuditProperty(List<AuditLabelInfo> auditLabelInfos, String propertyLabel) {
		String label = "";
		String pathLabel;

		for(AuditLabelInfo ali : auditLabelInfos) {
			pathLabel = getLabel(ali.getPropertyLabel());
			if(pathLabel != null) {
				if(ali.getObjectIdentifier() != null && !ali.getObjectIdentifier().isEmpty())
					pathLabel += " " + ali.getObjectIdentifier();
				if(!label.isEmpty())
					label += " ";
				label += pathLabel;
			}
		}
		if(!label.isEmpty())
			label += " - ";
		label += getLabel(propertyLabel);

		return label;
	}

	//Restituisce la label
	// se non in labelForAuditLabel restituisce "??key " + labelAudit + " not found??"
	// se presente in labelForAuditLabel
	//		se valorizzata il valore
	//		se non valorizzata null
	private String getLabel(String labelAudit) {
		String label = null;
		if(labelForAuditLabel.containsKey(labelAudit)) {
			label = labelForAuditLabel.get(labelAudit);
			if(label != null) {
				if(label.isEmpty())
					label = null;
				else
					label = messageSource.getMessage(label, null, Locale.getDefault());
					//messageSource.getMessage("label.dati_verbale", values, Locale.getDefault())
			}
		} else {
			//LOGGER.info("label_mancante");
			LOGGER.info(labelAudit);
			label = "??key " + labelAudit + " not found??";
		}
		return label;
	}

	@Override
	public void commitForCurrentUser(Object entity) {
		CurrentUser user = Utils.getAuthenticatedUser();
		if(user != null)
			javers.commit(user.getUsername(), entity);
		else
			javers.commit(workflowService.getSystemUsername(), entity);
	}

	@Override
	public void deleteForCurrrentUser(Object entity) {
		CurrentUser user = Utils.getAuthenticatedUser();
		if(user != null)
			javers.commitShallowDelete(user.getUsername(), entity);
		else
			javers.commitShallowDelete(workflowService.getSystemUsername(), entity);
	}

	@Override
	public void deleteByIdForCurrrentUser(Long entityId, Class javaClass) {
		CurrentUser user = Utils.getAuthenticatedUser();
		if(user != null)
			javers.commitShallowDeleteById(user.getUsername(), InstanceIdDTO.instanceId(entityId, javaClass));
		else
			javers.commitShallowDeleteById(workflowService.getSystemUsername(), InstanceIdDTO.instanceId(entityId, javaClass));
	}
}
