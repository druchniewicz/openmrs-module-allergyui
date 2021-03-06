package org.openmrs.module.allergyui.page.controller;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Allergies;
import org.openmrs.AllergyConstants;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.allergyui.extension.html.AllergyComparator;
import org.openmrs.module.appframework.domain.Extension;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.uicommons.UiCommonsConstants;
import org.openmrs.module.uicommons.util.InfoErrorMessageUtil;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.util.ByFormattedObjectComparator;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.openmrs.module.allergyui.AllergyUIConstants.ALLERGIES_PAGE_INCLUDE_FRAGMENT_EXTENSION_POINT;

public class AllergiesPageController {

	public void controller(@RequestParam("patientId") Patient patient,
                           @RequestParam(value = "returnUrl", required = false) String returnUrl,
                           PageModel model, UiUtils ui,
						   @SpringBean("appFrameworkService") AppFrameworkService appFrameworkService,
	                       @SpringBean("patientService") PatientService patientService) {
		
		Allergies allergies = patientService.getAllergies(patient);
		Comparator comparator = new AllergyComparator(new ByFormattedObjectComparator(ui));
		Collections.sort(allergies, comparator);

        if (StringUtils.isBlank(returnUrl)) {
            returnUrl = ui.pageLink("coreapps", "clinicianfacing/patient", Collections.singletonMap("patientId", (Object) patient.getId()));
        }

		List<Extension> includeFragments = appFrameworkService.getExtensionsForCurrentUser(ALLERGIES_PAGE_INCLUDE_FRAGMENT_EXTENSION_POINT);
		Collections.sort(includeFragments);
		model.addAttribute("includeFragments", includeFragments);

		model.addAttribute("patient", patient);
		model.addAttribute("allergies", allergies);
        model.addAttribute("returnUrl", returnUrl);
		model.addAttribute("hasModifyAllergiesPrivilege", Context.getAuthenticatedUser().hasPrivilege(AllergyConstants.PRIVILEGE_MODIFY_ALLERGIES));
	}
	
	public String post(@RequestParam("patientId") Patient patient,
	                   @RequestParam(value = "action", required = false) String action, 
	                   @RequestParam(value = "allergyId", required = false) Integer allergyId,
                       @RequestParam(value = "returnUrl", required = false) String returnUrl,
                       PageModel model,UiUtils ui,
	                   HttpSession session, @SpringBean("patientService") PatientService patientService) {

        if (StringUtils.isNotBlank(action)) {
			try {
				Allergies allergies = null;
				if ("confirmNoKnownAllergies".equals(action)) {
					allergies = new Allergies();
					allergies.confirmNoKnownAllergies();
				}
				else if ("deactivate".equals(action)) {
					allergies = new Allergies();
				}
				else if ("removeAllergy".equals(action)) {
					allergies = patientService.getAllergies(patient);
					allergies.remove(allergies.getAllergy(allergyId));
				}
				
				patientService.setAllergies(patient, allergies);
				
				InfoErrorMessageUtil.flashInfoMessage(session, "allergyui.message.success");
				
				return "redirect:allergyui/allergies.page?patientId=" + patient.getPatientId() + "&returnUrl=" + ui.urlEncode(returnUrl);
			}
			catch (Exception e) {
				session.setAttribute(UiCommonsConstants.SESSION_ATTRIBUTE_ERROR_MESSAGE, "allergyui.message.fail");
			}
		}
		
		model.addAttribute("allergies", patientService.getAllergies(patient));
        model.addAttribute("returnUrl", returnUrl);
		
		return null;
	}
}
