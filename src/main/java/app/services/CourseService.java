package app.services;

import app.models.entity.Course;
import app.models.service.CourseServiceModel;
import app.models.service.LectureServiceModel;
import app.models.service.UserServiceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {

    CourseServiceModel createCourse(String username, CourseServiceModel courseServiceModel);

    List<CourseServiceModel> findAllCoursesByAuthorId(String id);

    List<CourseServiceModel> findRecentCourses();

    List<CourseServiceModel> getAllCourses();

    CourseServiceModel enableCourse(String id);

    CourseServiceModel disableCourse(String id);

    List<CourseServiceModel> findAllCoursesWithStatusTrue();


    Page<Course> findAllCourses(Pageable pageable);

    List<CourseServiceModel> findAllCoursesInTopic(String id);

    CourseServiceModel findCourseById(String id);

    CourseServiceModel enrollCourse(CourseServiceModel courseServiceModel, UserServiceModel userServiceModel);

    boolean checkIfCourseContainStudent(CourseServiceModel courseServiceModel, UserServiceModel user);

    boolean checkIfUserIsGraduated(CourseServiceModel courseServiceModel, UserServiceModel user);

    List<LectureServiceModel> findAllLecturesForCourse(String id);


    Course findById(String id);



}
