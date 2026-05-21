package hr.performancemanagement.repository;

import hr.performancemanagement.entities.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    SystemSetting findSystemSettingBySettingKey(String settingKey);
}
