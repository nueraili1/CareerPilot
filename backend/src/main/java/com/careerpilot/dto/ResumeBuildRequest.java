package com.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;

public class ResumeBuildRequest {

    private AiRuntimeConfig aiConfig;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "求职意向不能为空")
    private String targetRole;

    private String education;
    private String skills;
    private String projects;
    private String experience;

    public AiRuntimeConfig getAiConfig() {
        return aiConfig;
    }

    public void setAiConfig(AiRuntimeConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getProjects() {
        return projects;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }
}
