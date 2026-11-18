package com.miniudemy.content.domain;

/** İçeriğin tarayıcıya sunuluş şekli (#110, #113): {@code Content-Disposition: inline} ya da {@code attachment}. */
public enum DeliveryMode {
    /** Tarayıcıda açılır (video oynatılır, PDF görüntülenir). */
    INLINE,
    /** İndirilir. */
    ATTACHMENT
}
