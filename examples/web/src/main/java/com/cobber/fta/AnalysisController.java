package com.cobber.fta;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@SessionAttributes("analysis")
public class AnalysisController {

	// Provides the initial "analysis" object when the session doesn't have one yet
	// (e.g. navigating directly to /types or /about before ever submitting a file).
	@ModelAttribute("analysis")
	public Analysis defaultAnalysis() {
		return new Analysis(LocaleContextHolder.getLocale());
	}

	@RequestMapping(value = "/analysis", method = RequestMethod.GET)
	public ModelAndView analysisForm(final Model model) {
		// Always start fresh so re-visiting the form doesn't show stale results.
		model.addAttribute("analysis", new Analysis(LocaleContextHolder.getLocale()));
		return new ModelAndView("analysis");
	}

	@RequestMapping(value = "/analysis", method = RequestMethod.POST)
	public ModelAndView analysisSubmit(@ModelAttribute Analysis analysis) {
		return new ModelAndView("result");
	}

	@RequestMapping(value = "/types", method = RequestMethod.GET)
	public ModelAndView typesForm() {
		return new ModelAndView("types");
	}

	@RequestMapping(value = "/about", method = RequestMethod.GET)
	public ModelAndView about() {
		return new ModelAndView("about");
	}

	@ControllerAdvice
	public class FileUploadExceptionAdvice {

		@ExceptionHandler(MaxUploadSizeExceededException.class)
		public ModelAndView handleMaxSizeException(
				MaxUploadSizeExceededException e,
				HttpServletRequest request,
				HttpServletResponse response) {
			return error("File too large!");
		}
	}

	private ModelAndView error(final String message) {
		final ModelAndView modelAndView = new ModelAndView("error");
		modelAndView.getModel().put("message", message);
		return modelAndView;
	}
}
