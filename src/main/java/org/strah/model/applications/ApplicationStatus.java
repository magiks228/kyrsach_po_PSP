package org.strah.model.applications;

public enum ApplicationStatus {
    NEW,            // создана, ждёт рассмотрения
    WAIT_PAYMENT,   // одобрена, ожидает оплаты
    PAID,           // клиент отметил «оплачено»
    DECLINED,       // отклонена
    FINISHED        // по ней сформирован полис
}
