package app.web.controllers.view;

import app.models.binding.CourseCreateBindingModel;
import app.models.service.CourseServiceModel;
import app.models.service.UserServiceModel;
import app.services.CloudinaryService;
import app.services.CourseService;
import app.services.TopicService;
import app.services.UserService;
import app.validations.anotations.PageTitle;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

import static app.constants.GlobalConstants.BINDING_RESULT;

@Controller
@RequestMapping("/courses")
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CourseController {
    private TopicService topicService;
    private CourseService courseService;
    private UserService userService;
    private CloudinaryService cloudinaryService;
    private ModelMapper modelMapper;

    @GetMapping("/create")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @PageTitle("Create Course")
    public String createCourse(Model model) {

        if (!model.containsAttribute("courseCreateBindingModel")) {
            model.addAttribute("courseCreateBindingModel", new CourseCreateBindingModel());
        }
        model.addAttribute("topicNames", this.topicService.getAllTopicNames());

        return "courses/create-course";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @PageTitle("Create Course")
    public String createCourseConfirm(@Valid @ModelAttribute("courseCreateBindingModel") CourseCreateBindingModel courseCreateBindingModel,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Principal principal) {
        System.out.println();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("courseCreateBindingModel", courseCreateBindingModel);
            redirectAttributes.addFlashAttribute(String.format(BINDING_RESULT + "courseCreateBindingModel"), bindingResult);
            return "redirect:/courses/create";
        }
        String coursePhotoURL;
        try {
            coursePhotoURL = cloudinaryService.uploadImage(courseCreateBindingModel.getCoursePhoto());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("imageSizeException", e.getMessage());
            return "redirect:/courses/create";
        }
        CourseServiceModel courseServiceModel = this.modelMapper.map(courseCreateBindingModel, CourseServiceModel.class);
        courseServiceModel.setCoursePhoto(coursePhotoURL);


        this.courseService.createCourse(principal.getName(), courseServiceModel);


        return "redirect:/courses/teacher-courses";
    }

    @GetMapping("/teacher-courses")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @PageTitle("Teacher Course")
    public String teacherCourses(Model model, Principal principal) {
        UserServiceModel userServiceModel = this.userService.findByName(principal.getName());

        List<CourseServiceModel> allCoursesByAuthorId = courseService.findAllCoursesByAuthorId(userServiceModel.getId());
        model.addAttribute("allCoursesByAuthorId", allCoursesByAuthorId);
        return "courses/teacher-courses";
    }

    @GetMapping("/allCourses")
    @PageTitle("All Courses")
    public String allCourses(Model model) {

        model.addAttribute("allCourses", courseService.findAllCoursesWithStatusTrue());
        model.addAttribute("allTopics", topicService.findAllTopics());
        model.addAttribute("top3RecentCourses", courseService.findRecentCourses());
        return "courses/allCourses";
    }

    @GetMapping("/allCoursesInTopic/{id}")
    @PageTitle("All Courses")
    public String allCoursesInTopic(@PathVariable("id") String id, Model model) {
        model.addAttribute("allCourses", courseService.findAllCoursesInTopic(id));
        model.addAttribute("allTopics", topicService.findAllTopics());
        model.addAttribute("top3RecentCourses", courseService.findRecentCourses());
        return "courses/allCourses";
    }

    @GetMapping("/courseDetails/{id}")
    @PageTitle("Course Details")
    public String courseDetails(@PathVariable("id") String id, Model model) {
        CourseServiceModel courseServiceModel = this.courseService.findCourseById(id);
        model.addAttribute("course",courseServiceModel);
        model.addAttribute("teacher", this.userService.findTeacher(courseServiceModel.getAuthor().getId()));
        model.addAttribute("allTopics", topicService.findAllTopics());
        model.addAttribute("top3RecentCourses", courseService.findRecentCourses());
        return "courses/courseDetails";
    }
}
