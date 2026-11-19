"use client";

import { useParams } from "next/navigation";
import { CourseForm } from "../../CourseForm";

/** Adresteki kurs kimliğini forma verir; kimlik her değiştiğinde form baştan kurulur. */
export function EditCourse() {
  const params = useParams<{ id: string }>();
  const courseId = Number(params.id);
  if (!Number.isInteger(courseId) || courseId <= 0) {
    return (
      <p className="auth-error" role="alert">
        Kurs bulunamadı.
      </p>
    );
  }
  return <CourseForm key={courseId} courseId={courseId} />;
}
