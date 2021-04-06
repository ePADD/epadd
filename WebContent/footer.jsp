
<script>
    var old_onload_if_any = window.onload;
    var new_on_load = function(){

    }
    window.onload = function () {
            if(old_onload_if_any)
                old_onload_if_any()
            //method to add footer
        var d = document.getElementById("common-footer");
            d.style.display="block";

    }
</script>
<br/>
<footer id="common-footer" hidden>
    <div style="">
        <div style="padding-top:12px;color:#ababab;font-size:12px;width:100%"><a href="https://library.stanford.edu/projects/epadd">ePADD</a> <% out.println(edu.stanford.epadd.Version.version);%> &copy; Stanford University</div>
    </div>
</footer>
