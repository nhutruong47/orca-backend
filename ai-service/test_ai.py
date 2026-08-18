import sys
import os

from app.models import ExtractRequest, PlanRequest
from app.gemini_ai import extract, plan

def run_test():
    try:
        ext_req = ExtractRequest(text="Rang 120kg Arabica trước 17:00 hôm nay")
        ext_res = extract(ext_req)
        print("Extract Response:")
        print(ext_res.model_dump_json(indent=2))

        if ext_res.intent != "UNKNOWN" and not ext_res.missingFields:
            plan_req = PlanRequest(
                intent=ext_res.intent,
                fields=ext_res.fields,
                members=[]
            )
            plan_res = plan(plan_req)
            print("\nPlan Response:")
            print(plan_res.model_dump_json(indent=2))
        else:
            print("\nCannot plan, missing fields or unknown intent.")
    except Exception as e:
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    run_test()
