package app.web.controllers.view;

import app.models.binding.assignment.AssignmentSolutionAddBindingModel;
import app.models.binding.course.CourseCreateBindingModel;
import app.models.entity.Course;
import app.models.service.CourseServiceModel;
import app.models.service.RoleServiceModel;
import app.models.service.UserServiceModel;
import app.services.*;
import app.validations.anotations.PageTitle;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
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
    @PageTitle("Teacher Courses")
    public String teacherCourses(Model model, Principal principal) {
        UserServiceModel userServiceModel = this.userService.findByName(principal.getName());
        List<CourseServiceModel> allCoursesByAuthorId = courseService.findAllCoursesByAuthorId(userServiceModel.getId());
        model.addAttribute("allCoursesByAuthorId", allCoursesByAuthorId);
        return "courses/teacher-courses";
    }

    @GetMapping("/teacher-check-courses")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @PageTitle("Teacher Courses")
    public String teacherCheckAssignmentCourses(Model model, Principal principal) {
        UserServiceModel userServiceModel = this.userService.findByName(principal.getName());
        List<CourseServiceModel> allCoursesByAuthorId = courseService.findAllCoursesByAuthorId(userServiceModel.getId());

        model.addAttribute("allCoursesByAuthorId", allCoursesByAuthorId);

        return "courses/teacher-all-courses-table";
    }


    @GetMapping("/enrolledCourses")
    @PreAuthorize("hasAnyRole('ROLE_STUDENT','ROLE_ADMIN')")
    @PageTitle("Student Courses")
    public String studentCourses(Model model, Principal principal) {


        UserServiceModel userServiceModel = this.userService.findByName(principal.getName());
        List<CourseServiceModel> allCoursesByUserId = userService.findAllCoursesByUserId(userServiceModel.getId());


        model.addAttribute("allCoursesByUserId", allCoursesByUserId);

        return "courses/student-enrolled-courses";
    }

    @GetMapping("/completedCourses")
    @PreAuthorize("hasAnyRole('ROLE_STUDENT','ROLE_ADMIN')")
    @PageTitle("Completed Courses")
    public String completedCourses(Model model, Principal principal) {


        UserServiceModel userServiceModel = this.userService.findByName(principal.getName());
        List<CourseServiceModel> allCompletedCoursesByUserId = userService.findAllCompletedCourses(userServiceModel.getId());


        model.addAttribute("allCompletedCoursesByUserId", allCompletedCoursesByUserId);

        return "courses/student-completed-courses";
    }

    @GetMapping("/allCourses")
    @PageTitle("All Courses")
    public String allCourses(Model model, @PageableDefault( size = 3) Pageable pageable) {

        Page<Course> courses = this.courseService.findCourses(pageable);
        model.addAttribute("allCourses",courses);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("allCourses", courseService.findAllCoursesWithStatusTrue());
        model.addAttribute("allTopics", topicService.findAllTopics());
        model.addAttribute("top3RecentCourses", courseService.findRecentCourses());
        return "courses/all-courses";
    }

    @GetMapping("/allCoursesInTopic/{id}")
    @PageTitle("All Courses In Topic")
    public String allCoursesInTopic(@PathVariable("id") String id, Model model) {

        model.addAttribute("allCourses", courseService.findAllCoursesInTopic(id));
        model.addAttribute("allTopics", topicService.findAllTopics());
        model.addAttribute("top3RecentCourses", courseService.findRecentCourses());

        return "courses/all-courses";
    }


    @GetMapping("/courseDetails/{id}")
    @PageTitle("Course Details")
    public String courseDetails(@PathVariable("id") String id,
                                Model model,
                                Principal principal,
                                AssignmentSolutionAddBindingModel solutionAddBindingModel) {

        CourseServiceModel courseServiceModel = this.courseService.findCourseById(id);

        if (principal != null) {
            UserServiceModel user = userService.findByName(principal.getName());
            boolean contains = courseService.checkIfCourseContainStudent(courseServiceModel, user);
            boolean graduated = courseService.checkIfUserIsGraduated(courseServiceModel, user);
            boolean isAuthor = courseServiceModel.getAuthor().getId().equals(user.getId());
            if (!model.containsAttribute("solutionAddBindingModel")) {
                model.addAttribute("solutionAddBindingModel", new AssignmentSolutionAddBindingModel());
            }
            model.addAttribute("author", isAuthor);
            model.addAttribute("graduated", graduated);
            model.addAttribute("containStudent", contains);

        }


        if (principal != null) {
            RoleServiceModel role = this.userService.findByName(principal.getName()).getAuthorities().stream().findFirst().orElse(null);
            boolean isRoot = role.getAuthority().equals("ROLE_ROOT_ADMIN");
            boolean isAdmin = role.getAuthority().equals("ROLE_ADMIN");
            model.addAttribute("isRoot", isRoot);
            model.addAttribute("isAdmin", isAdmin);
        }
        model.addAttribute("course", courseServiceModel);
        model.addAttribute("teacher", this.userService.findTeacher(courseServiceModel.getAuthor().getId()));
        model.addAttribute("lectures", courseService.findAllLecturesForCourse(id));
        model.addAttribute("allTopics", topicService.findAllTopics());
        model.addAttribute("top3RecentCourses", courseService.findRecentCourses());
        model.addAttribute("localDateTime", LocalDateTime.now());
        return "courses/course-details";
    }


    @GetMapping("/enrollCourse/{id}")
    public String enrollCourse(@PathVariable("id") String id, Principal principal) {
        CourseServiceModel courseServiceModel = this.courseService.findCourseById(id);
        UserServiceModel userServiceModel = this.userService.findByName(principal.getName());
        courseService.enrollCourse(courseServiceModel, userServiceModel);
        return "redirect:/courses/courseDetails/" + id;
    }
}
