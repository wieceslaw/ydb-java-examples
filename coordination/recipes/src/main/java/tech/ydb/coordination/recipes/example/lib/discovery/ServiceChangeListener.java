package tech.ydb.coordination.recipes.example.lib.discovery;

import java.util.List;

/**
 * Интерфейс слушателя изменений для обработки обновлений в списке экземпляров сервиса.
 */
interface ServiceChangeListener {
    /**
     * Вызывается при изменении списка экземпляров сервиса.
     *
     * @param serviceName имя сервиса
     * @param newInstances новый список адресов экземпляров
     */
    void onChange(String serviceName, List<String> newInstances);
}
