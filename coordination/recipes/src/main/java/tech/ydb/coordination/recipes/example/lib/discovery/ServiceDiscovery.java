package tech.ydb.coordination.recipes.example.lib.discovery;

import java.util.Set;

public interface ServiceDiscovery {

    /**
     * Регистрирует новый сервис с заданным именем и адресом.
     *
     * @param serviceName имя сервиса
     * @param serviceAddress адрес сервиса (например, URL или IP)
     * @throws ServiceDiscoveryException если регистрация не удалась
     */
    void registerService(String serviceName, String serviceAddress) throws ServiceDiscoveryException;

    /**
     * Удаляет регистрацию сервиса с заданным именем и адресом.
     *
     * @param serviceName имя сервиса
     * @param serviceAddress адрес сервиса
     * @throws ServiceDiscoveryException если удаление не удалось
     */
    void unregisterService(String serviceName, String serviceAddress) throws ServiceDiscoveryException;

    /**
     * Получает список адресов для всех зарегистрированных экземпляров сервиса с заданным именем.
     *
     * @param serviceName имя сервиса
     * @return список адресов зарегистрированных экземпляров
     * @throws ServiceDiscoveryException если получение списка не удалось
     */
    Set<String> getServiceInstances(String serviceName) throws ServiceDiscoveryException;

    /**
     * Подписывается на изменения в списке экземпляров для заданного сервиса.
     *
     * @param serviceName имя сервиса
     * @param listener слушатель изменений
     * @throws ServiceDiscoveryException если подписка не удалась
     */
    void subscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException;

    /**
     * Отменяет подписку на изменения для заданного сервиса.
     *
     * @param serviceName имя сервиса
     * @param listener слушатель изменений
     * @throws ServiceDiscoveryException если отмена подписки не удалась
     */
    void unsubscribe(String serviceName, ServiceChangeListener listener) throws ServiceDiscoveryException;

}

