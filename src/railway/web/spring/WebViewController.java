package railway.web.spring;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to route base requests to index.html
 */
@Controller
public class WebViewController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
