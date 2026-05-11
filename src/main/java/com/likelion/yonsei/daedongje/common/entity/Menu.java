@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_sold_out", nullable = false)
    private Boolean isSoldOut = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder
    public Menu(
            Booth booth,
            String name,
            String description,
            Integer price,
            String imageUrl,
            Boolean isSoldOut,
            Integer displayOrder) {
        this.booth = booth;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isSoldOut = isSoldOut;
        this.displayOrder = displayOrder;
    }

    public void updateSoldOut(Boolean isSoldOut) {
        this.isSoldOut = isSoldOut;
    }
}