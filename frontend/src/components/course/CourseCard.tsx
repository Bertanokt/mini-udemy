import Link from "next/link";
import { API_BASE_URL } from "@/lib/api/client";
import { LEVEL_LABELS, type CourseSummaryResponse } from "@/lib/api/catalog";

export function formatDuration(totalSeconds: number): string {
  const minutes = Math.round(totalSeconds / 60);
  if (minutes < 60) return `${minutes} dk`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest === 0 ? `${hours} sa` : `${hours} sa ${rest} dk`;
}

interface Props {
  course: CourseSummaryResponse;
  instructorHref: string;
}

export function CourseCard({ course, instructorHref }: Props) {
  const cover = course.coverUrl ? `${API_BASE_URL}${course.coverUrl}` : null;
  return (
    <article className="course-card">
      <Link href={`/courses/${course.id}`} className="course-card-link">
        <div
          className="course-cover"
          role="img"
          aria-label={`${course.title} kapak görseli`}
          style={cover ? { backgroundImage: `url("${cover}")` } : undefined}
        >
          {!cover && <span className="course-cover-placeholder">{course.title.charAt(0)}</span>}
        </div>
        <h2 className="course-title">{course.title}</h2>
      </Link>
      <Link href={instructorHref} className="course-instructor" title="Bu eğitmenin kursları">
        {course.instructor.fullName}
      </Link>
      <div className="course-tags">
        <span className="tag">{course.category.name}</span>
        <span className="tag">{LEVEL_LABELS[course.level]}</span>
      </div>
      <p className="course-meta">
        {course.lessonCount} ders · {formatDuration(course.totalDurationSeconds)}
        {course.averageRating != null && ` · ★ ${course.averageRating.toFixed(1)} (${course.reviewCount})`}
      </p>
    </article>
  );
}
