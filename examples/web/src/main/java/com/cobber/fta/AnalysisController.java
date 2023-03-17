package com.cobber.fta;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AnalysisController {

	@RequestMapping(value = "/analysis", method = RequestMethod.GET)
	public ModelAndView analysisForm(Model model) {
		model.addAttribute("analysis", new Analysis(LocaleContextHolder.getLocale()));
		return new ModelAndView("analysis");
	}

	@RequestMapping(value = "/analysis", method = RequestMethod.POST)
	public ModelAndView analysisSubmit(@ModelAttribute Analysis analysis, Model model) {
		model.addAttribute("analysis", analysis);
		return new ModelAndView("result");
	}

	@ControllerAdvice
	public class FileUploadExceptionAdvice {

		@ExceptionHandler(MaxUploadSizeExceededException.class)
		public ModelAndView handleMaxSizeException(
				MaxUploadSizeExceededException e,
				HttpServletRequest request,
				HttpServletResponse response) {
			return Error("File too large!");
		}
	}

	private ModelAndView Error(final String message) {
		ModelAndView modelAndView = new ModelAndView("error");
		modelAndView.getModel().put("message", message);
		return modelAndView;
	}
}
